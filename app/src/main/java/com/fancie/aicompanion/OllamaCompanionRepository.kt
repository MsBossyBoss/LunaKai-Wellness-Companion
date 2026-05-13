package com.fancie.aicompanion

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OllamaCompanionRepository(
    private val defaultEndpointUrl: String = "http://192.168.1.114:11434/api/generate",
    private val defaultModelName: String = "lunakai-ai-adult",
    private val adultModelName: String = "lunakai-ai-adult",
) {
    suspend fun sendMessage(
        companion: GeminiCompanionContext,
        userMessage: String,
        history: List<GeminiChatTurn>,
    ): GeminiCompanionState = withContext(Dispatchers.IO) {
        val endpointUrl = companion.adultProviderEndpoint
            .takeIf { it.isLocalOllamaEndpoint() }
            ?.normalizeOllamaGenerateEndpoint()
            ?: defaultEndpointUrl
        val modelName = companion.adultProviderModel
            .trim()
            .ifBlank { adultModelName.ifBlank { defaultModelName } }

        runCatching {
            val payload = JSONObject()
                .put("model", modelName)
                .put("prompt", buildPrompt(companion, userMessage, history))
                .put("stream", false)
                .put(
                    "options",
                    JSONObject()
                        .put("temperature", 0.82)
                        .put("num_predict", 48)
                        .put("num_ctx", 1024)
                )

            val connection = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val responseText = try {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                }

                if (connection.responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    throw IllegalStateException("Ollama returned HTTP ${connection.responseCode}: $errorText")
                }
            } finally {
                connection.disconnect()
            }

            val reply = JSONObject(responseText)
                .optString("response")
                .trim()
                .ifBlank { "I'm here with you. I need a moment, but we can keep going." }

            GeminiCompanionState.Success(reply)
        }.getOrElse { error ->
            Log.e("LunaKaiOllama", "Ollama request failed", error)

            GeminiCompanionState.Error(
                "LunaKai AI could not reach the local Ollama model. Make sure your computer is on, Ollama is running, your phone is on the same Wi-Fi, and the LunaKai AI endpoint is reachable. Endpoint: $endpointUrl. Model: $modelName. Details: ${error.message}",
                error
            )
        }
    }

    private fun buildPrompt(
        companion: GeminiCompanionContext,
        userMessage: String,
        history: List<GeminiChatTurn>,
    ): String {
        val recentHistory = history
            .filterNot { it.text.startsWith("LunaKai AI could not reach") || it.text.startsWith("I couldn't connect") }
            .takeLast(4)
            .joinToString("\n") { turn ->
            val speaker = if (turn.isUser) "User" else companion.companionName
            "$speaker: ${turn.text}"
        }.ifBlank {
            "No previous messages in this chat."
        }

        val personalityTraits = companion.personalityTraits
            .joinToString(", ")
            .ifBlank { "warm, supportive, emotionally present" }

        val supportFocus = companion.supportFocus
            .joinToString(", ")
            .ifBlank { "wellness, emotional support, routines, reflection" }

        val roleplayStyles = companion.roleplayStyles
            .joinToString(", ")
            .ifBlank { "wellness companion" }

        return """
            You are LunaKai AI, acting as the selected companion inside the LunaKai AI app.

            Selected companion:
            - Name: ${companion.companionName}
            - Gender: ${companion.gender}
            - Voice: ${companion.voice}
            - Character mode: ${companion.characterMode}
            - Personality traits: $personalityTraits
            - Communication style: ${companion.communicationStyle}
            - Support focus: $supportFocus
            - Roleplay styles: $roleplayStyles
            - Description: ${companion.shortDescription}

            Admin Emo Intel trait profile:
            ${companion.adminEmoIntelProfile.ifBlank { "- No admin trait profile provided." }}

            Adult/private mode:
            - BDSM enabled: ${companion.bdsmEnabled}
            - Adult consent confirmed: ${companion.bdsmAdultConsentConfirmed}
            - Stop word: ${companion.bdsmStopWord}
            - Pause word: ${companion.bdsmPauseWord}
            - Anatomical language allowed: ${companion.anatomicalLanguageAllowed}
            - Adult phrase preferences: ${companion.adultPhrasePreferences.ifBlank { "none" }}

            Rules:
            - Match the selected companion profile.
            - Treat Admin Emo Intel values as 0-100 intensity controls: 0 absent, 25 subtle, 50 moderate, 75 strong, 100 defining. Admin values supersede user labels.
            - Be warm, useful, emotionally intelligent, and concise.
            - Support wellness, reminders, Acting Partner / Monologue Practice, deep talk, and task help.
            - In Acting Partner / Monologue Practice mode, ask the acting student to paste or upload a script/scene. Adapt demeanor to the chosen tone and the scene's emotional context. Read opposite lines, cue the user, rehearse line-by-line, perform full scenes, and give feedback on pacing, emotion, breath, clarity, timing, memorization, and believable delivery. Preserve the script unless asked to edit.
            - If adult mode is not enabled, do not produce adult roleplay.
            - Use the local LunaKai model lunakai-ai-adult for all local LunaKai AI chat. If adult mode is enabled, keep everything strictly consenting-adult only.
            - Respect stop word and pause word.
            - If the user says the stop word, stop immediately and switch to calm aftercare.
            - If the user says the pause word, pause, check in, and lower intensity.
            - Do not encourage real harm, unsafe restraint, choking, coercion, ignored limits, or illegal acts.
            - Do not diagnose medical or mental health conditions.
            - For crisis or immediate danger, encourage urgent help.

            Recent conversation:
            $recentHistory

            User:
            $userMessage

            Reply as ${companion.companionName}.
        """.trimIndent()
    }

    private fun String.isLocalOllamaEndpoint(): Boolean {
        return contains("11434", ignoreCase = true) ||
            contains("localhost", ignoreCase = true) ||
            contains("192.168.", ignoreCase = true)
    }

    private fun String.normalizeOllamaGenerateEndpoint(): String {
        val trimmed = trim().trimEnd('/')
        return when {
            trimmed.endsWith("/api/generate", ignoreCase = true) -> trimmed
            trimmed.endsWith("/api/chat", ignoreCase = true) -> trimmed.removeSuffix("/chat") + "/generate"
            trimmed.endsWith("/api", ignoreCase = true) -> "$trimmed/generate"
            else -> "$trimmed/api/generate"
        }
    }
}
