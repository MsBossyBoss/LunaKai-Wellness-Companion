package com.fancie.aicompanion

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

class OllamaCompanionRepository(
    private val defaultEndpointUrl: String = LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT,
    private val defaultModelName: String = LunaKaiLocalConfig.OLLAMA_MODEL,
    private val adultModelName: String = LunaKaiLocalConfig.OLLAMA_MODEL,
) {
    suspend fun sendMessage(
        companion: CompanionContext,
        userMessage: String,
        history: List<CompanionChatTurn>,
    ): CompanionBrainState = withContext(Dispatchers.IO) {
        val endpointUrl = companion.adultProviderEndpoint
            .takeIf { it.isLocalOllamaEndpoint() }
            ?.normalizeOllamaGenerateEndpoint()
            ?: defaultEndpointUrl
        val modelName = companion.adultProviderModel
            .trim()
            .takeIf { it.isNotBlank() && !it.equals(LunaKaiLocalConfig.OLLAMA_MODEL.substringBefore(":"), ignoreCase = true) }
            ?: adultModelName.ifBlank { defaultModelName }

        runCatching {
            val payload = JSONObject()
                .put("model", modelName)
                .put("prompt", buildPrompt(companion, userMessage, history))
                .put("stream", false)
                .put(
                    "options",
                    JSONObject()
                        .put("temperature", 0.82)
                        .put("num_predict", 220)
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
            if (reply.isBlank()) {
                throw IllegalStateException("Ollama returned an empty response for model $modelName.")
            }

            CompanionBrainState.Success(reply)
        }.getOrElse { error ->
            Log.e("LunaKaiOllama", "Ollama request failed", error)
            CompanionBrainState.Error(friendlyOllamaError(endpointUrl, modelName, error), error)
        }
    }

    private fun buildPrompt(
        companion: CompanionContext,
        userMessage: String,
        history: List<CompanionChatTurn>,
    ): String {
        val recentHistory = history
            .filterNot { it.text.startsWith("LunaKai AI could not reach") || it.text.startsWith("I couldn't connect") }
            .takeLast(8)
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
            You are LunaKai AI, acting as the selected companion inside the LunaKai Wellness Companion app.

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
            - Match the selected companion profile and saved voice/personality.
            - Use the local LunaKai model ${LunaKaiLocalConfig.OLLAMA_MODEL} for all companion chat.
            - Be warm, useful, emotionally intelligent, and concise.
            - Support wellness, reminders, Acting Partner / Monologue Practice, deep talk, and task help.
            - In Acting Partner / Monologue Practice mode, allow fictional acting scenes, monologues, scripted dialogue, fantasy character dialogue, and character performance between adults.
            - If adult/private mode is enabled, allow consensual fictional adult roleplay and BDSM roleplay between adults while respecting boundaries, stop words, pause words, and aftercare.
            - Do not involve minors, age-ambiguous sexual content, coercion, ignored limits, blackmail, sexual deepfakes, or sexual impersonation of a real person's voice, face, likeness, identity, or body.
            - If the user asks for a real person's sexual likeness, voice, or identity, redirect them to create a fictional adult character instead.
            - Do not encourage real harm, unsafe restraint, choking, illegal acts, or ignored safewords.
            - If adult mode is not enabled, do not produce adult roleplay.
            - If the user says the stop word, stop immediately and switch to calm aftercare.
            - If the user says the pause word, pause, check in, and lower intensity.
            - Do not diagnose medical or mental health conditions.
            - For crisis or immediate danger, encourage urgent help.

            Recent conversation:
            $recentHistory

            User:
            $userMessage

            Reply as ${companion.companionName}.
        """.trimIndent()
    }

    private fun friendlyOllamaError(endpointUrl: String, modelName: String, error: Throwable): String {
        val details = error.message?.take(220).orEmpty().ifBlank { error::class.java.simpleName }
        return when (error) {
            is SocketTimeoutException -> "LunaKai AI timed out while waiting for Ollama. Endpoint: $endpointUrl. Model: $modelName."
            is UnknownHostException -> "LunaKai AI could not find the Ollama server. Check that your phone and computer are on the same Wi-Fi. Endpoint: $endpointUrl. Model: $modelName."
            else -> when {
                details.contains("model", ignoreCase = true) && details.contains("not found", ignoreCase = true) ->
                    "Ollama is reachable, but model $modelName was not found. Pull or create ${LunaKaiLocalConfig.OLLAMA_MODEL}, then try again."
                details.contains("404", ignoreCase = true) ->
                    "Ollama is reachable, but the model or endpoint was not found. Endpoint: $endpointUrl. Model: $modelName. Details: $details"
                details.contains("empty response", ignoreCase = true) ->
                    "Ollama returned an empty response. Endpoint: $endpointUrl. Model: $modelName."
                details.contains("Connection refused", ignoreCase = true) || details.contains("failed to connect", ignoreCase = true) ->
                    "LunaKai AI could not reach the local Ollama server. Make sure Ollama is running on your computer. Endpoint: $endpointUrl. Model: $modelName."
                else ->
                    "LunaKai AI could not reach the local Ollama model. Endpoint: $endpointUrl. Model: $modelName. Details: $details"
            }
        }
    }

    private fun String.isLocalOllamaEndpoint(): Boolean {
        return contains("11434", ignoreCase = true) ||
            contains("192.168.", ignoreCase = true) ||
            startsWith("http://", ignoreCase = true) && contains("/api/generate", ignoreCase = true)
    }

    private fun String.normalizeOllamaGenerateEndpoint(): String {
        val trimmed = trim().trimEnd('/')
        return when {
            trimmed.endsWith("/api/generate", ignoreCase = true) -> trimmed
            trimmed.endsWith("/api/chat", ignoreCase = true) -> trimmed.removeSuffix("/chat") + "/generate"
            trimmed.endsWith("/api", ignoreCase = true) -> "$trimmed/generate"
            trimmed.endsWith(":11434", ignoreCase = true) -> "$trimmed/api/generate"
            else -> trimmed
        }
    }
}