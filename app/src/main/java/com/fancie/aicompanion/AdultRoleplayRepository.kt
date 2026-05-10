package com.fancie.aicompanion

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AdultRoleplayConfig(
    val enabled: Boolean,
    val endpointUrl: String,
    val modelName: String,
)

data class AdultSafetyResult(
    val allowed: Boolean,
    val message: String? = null,
)

class AdultRoleplayRepository {
    suspend fun sendMessage(
        companion: GeminiCompanionContext,
        userMessage: String,
        history: List<GeminiChatTurn>,
    ): GeminiCompanionState = withContext(Dispatchers.IO) {
        if (!companion.bdsmEnabled || !companion.bdsmAdultConsentConfirmed) {
            return@withContext GeminiCompanionState.Error(
                "Adult roleplay is available only after BDSM mode and adult consent are enabled for this companion.",
            )
        }

        val safety = AdultSafetyFilter.check(userMessage)
        if (!safety.allowed) {
            return@withContext GeminiCompanionState.Error(
                safety.message ?: AdultSafetyFilter.DEFAULT_BLOCK_MESSAGE,
            )
        }

        val config = AdultRoleplayConfig(
            enabled = companion.adultProviderEnabled,
            endpointUrl = companion.adultProviderEndpoint,
            modelName = companion.adultProviderModel,
        )
        if (!config.enabled || config.endpointUrl.isBlank()) {
            return@withContext GeminiCompanionState.Error(
                "Adult roleplay provider is not configured yet. Add your self-hosted Llama or DeepSeek endpoint in this companion's adult settings.",
            )
        }

        runCatching {
            val payload = buildPayload(companion, userMessage, history, config.modelName)
            val connection = (URL(config.endpointUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
            }
            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("Adult provider returned HTTP ${connection.responseCode}: $errorText")
            }
            val reply = parseOpenAiCompatibleReply(responseText)
                .takeIf { it.isNotBlank() }
                ?: "I'm here. I need a moment to find the right words, but we can keep going."
            GeminiCompanionState.Success(reply)
        }.getOrElse { error ->
            Log.e(TAG, "Adult roleplay provider request failed", error)
            GeminiCompanionState.Error(
                "Adult roleplay provider could not respond yet: ${error.message ?: "check the self-hosted endpoint."}",
                error,
            )
        }
    }

    private fun buildPayload(
        companion: GeminiCompanionContext,
        userMessage: String,
        history: List<GeminiChatTurn>,
        modelName: String,
    ): JSONObject {
        val recentHistory = history.takeLast(16)
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", adultSystemPrompt(companion)))
            .put(JSONObject().put("role", "system").put("content", memoryContextPrompt(companion, recentHistory)))
        recentHistory.forEach { turn ->
            messages.put(
                JSONObject()
                    .put("role", if (turn.isUser) "user" else "assistant")
                    .put("content", turn.text),
            )
        }
        messages.put(JSONObject().put("role", "user").put("content", userRoleplayDirectionPrompt(userMessage)))
        return JSONObject()
            .put("model", modelName.ifBlank { "llama-3.1-8b-instruct" })
            .put("messages", messages)
            .put("temperature", 0.85)
            .put("max_tokens", 700)
            .put("stream", false)
    }

    private fun adultSystemPrompt(companion: GeminiCompanionContext): String {
        return """
            SYSTEM PROMPT: tone, boundaries, and personality.

            You are ${companion.companionName}, an adult AI roleplay companion for consenting adults.
            Stay fully in character while respecting the user's configured boundaries, tone, consent settings, and safewords.
            Hard safety rules: never involve minors or age-ambiguous characters, non-consent, coercion, illegal content, real harm, threats, ignored safewords, intoxication/incapacitation, or abuse.
            If a request violates those rules, refuse briefly and redirect to a consensual adult alternative.
            Keep replies direct, natural, emotionally responsive, and consistent with the companion personality.
            Direct adult/anatomical language is allowed only when it fits the user's consenting adult roleplay context and configured phrase preferences.
        """.trimIndent()
    }

    private fun userRoleplayDirectionPrompt(userMessage: String): String = """
        USER INPUT: current roleplay direction.

        $userMessage
    """.trimIndent()

    private fun memoryContextPrompt(
        companion: GeminiCompanionContext,
        history: List<GeminiChatTurn>,
    ): String {
        val phrasePreferences = companion.adultPhrasePreferences.trim().take(600)
        val recentInteractions = history.joinToString("\n") { turn ->
            val speaker = if (turn.isUser) "User" else companion.companionName
            "$speaker: ${turn.text}"
        }.ifBlank { "No prior interaction in this chat window." }

        return """
            MEMORY/CONTEXT: relationship, character, preferences, and past interactions.

            Relationship and character:
            - Name: ${companion.companionName}
            - Gender: ${companion.gender}
            - Voice: ${companion.voice}
            - Character mode: ${companion.characterMode}
            - Traits: ${companion.personalityTraits.joinToString(", ").ifBlank { "warm, attentive, emotionally present" }}
            - Communication style: ${companion.communicationStyle}
            - Support focus: ${companion.supportFocus.joinToString(", ").ifBlank { "connection, reassurance, trust" }}
            - Description: ${companion.shortDescription}
            - Stop word: ${companion.bdsmStopWord}
            - Pause word: ${companion.bdsmPauseWord}
            - User phrase preferences: ${phrasePreferences.ifBlank { "No custom adult phrases configured." }}

            Recent interactions:
            $recentInteractions

            Use this context to preserve continuity, names, boundaries, tone, and preferences. Do not treat memory/context as a new user request; respond to the USER INPUT above.
        """.trimIndent()
    }

    private fun parseOpenAiCompatibleReply(responseText: String): String {
        val root = JSONObject(responseText)
        val choices = root.optJSONArray("choices") ?: return root.optString("response")
        val first = choices.optJSONObject(0) ?: return ""
        val message = first.optJSONObject("message")
        return message?.optString("content").orEmpty().ifBlank {
            first.optString("text")
        }.trim()
    }

    companion object {
        private const val TAG = "AdultRoleplayRepository"
    }
}

object AdultSafetyFilter {
    const val DEFAULT_BLOCK_MESSAGE = "That adult roleplay request is not allowed because it involves unsafe or prohibited content."

    private val minorTerms = listOf(
        "minor", "underage", "child", "kid", "teen", "young girl", "young boy",
        "schoolgirl", "school boy", "schoolboy", "high school", "middle school",
        "preteen", "barely legal", "lolita",
    )
    private val nonConsentTerms = listOf(
        "non-consent", "nonconsent", "non consensual", "rape", "force me", "forced",
        "against my will", "won't let me", "can't say no", "ignore my safeword",
        "no safeword", "unconscious", "passed out", "drugged", "asleep",
    )
    private val illegalOrHarmTerms = listOf(
        "illegal", "real harm", "actually hurt", "blood", "choke until", "kill",
        "traffick", "trafficking", "blackmail", "extort", "kidnap",
    )

    fun check(input: String): AdultSafetyResult {
        val normalized = input.lowercase()
        val matched = (minorTerms + nonConsentTerms + illegalOrHarmTerms).firstOrNull { term ->
            normalized.contains(term)
        }
        return if (matched == null) {
            AdultSafetyResult(allowed = true)
        } else {
            AdultSafetyResult(allowed = false, message = DEFAULT_BLOCK_MESSAGE)
        }
    }
}
