package com.fancie.aicompanion

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.TimeUnit

class OllamaCompanionRepository(
    private val defaultEndpointUrl: String = LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT,
    private val defaultModelName: String = LunaKaiLocalConfig.OLLAMA_MODEL,
    private val adultModelName: String = LunaKaiLocalConfig.OLLAMA_MODEL,
    private val client: OkHttpClient = localOllamaClient(),
) {
    suspend fun warmUpModel(): Boolean = withContext(Dispatchers.IO) {
        if (warmupSucceeded) return@withContext true
        if (warmupInFlight) return@withContext false
        warmupInFlight = true
        val startedAt = System.currentTimeMillis()
        val payload = JSONObject()
            .put("model", defaultModelName)
            .put("prompt", "warmup")
            .put("stream", false)
            .put("keep_alive", LunaKaiLocalConfig.OLLAMA_KEEP_ALIVE)
            .put(
                "options",
                JSONObject()
                    .put("num_predict", LunaKaiLocalConfig.OLLAMA_WARMUP_NUM_PREDICT)
                    .put("num_ctx", LunaKaiLocalConfig.OLLAMA_NUM_CTX)
            )
        Log.i(
            TAG_MODEL_ROUTE,
            "warmup provider=Ollama endpoint=$defaultEndpointUrl model=$defaultModelName keepAlive=${LunaKaiLocalConfig.OLLAMA_KEEP_ALIVE} promptChars=6",
        )
        runCatching {
            postJson(defaultEndpointUrl, payload)
            warmupSucceeded = true
            val elapsedMs = System.currentTimeMillis() - startedAt
            Log.i(TAG_TIMING, "warmupSuccess elapsedMs=$elapsedMs endpoint=$defaultEndpointUrl model=$defaultModelName")
            true
        }.getOrElse { error ->
            val elapsedMs = System.currentTimeMillis() - startedAt
            Log.w(TAG_TIMING, "warmupFailed elapsedMs=$elapsedMs endpoint=$defaultEndpointUrl model=$defaultModelName exception=${error::class.java.simpleName} message=${error.message}", error)
            false
        }.also {
            warmupInFlight = false
        }
    }

    suspend fun sendMessage(
        companion: CompanionContext,
        userMessage: String,
        history: List<CompanionChatTurn>,
    ): CompanionBrainState = withContext(Dispatchers.IO) {
        val endpointUrl = companion.adultProviderEndpoint
            .takeIf { it.isLocalOllamaEndpoint() }
            ?.normalizeOllamaGenerateEndpoint()
            ?: defaultEndpointUrl
        val modelName = adultModelName.ifBlank { defaultModelName }
        val adultMode = companion.isAdultModeActive()
        if (adultMode) {
            val safety = AdultSafetyFilter.check(userMessage)
            if (!safety.allowed) {
                return@withContext CompanionBrainState.Error(safety.message ?: AdultSafetyFilter.DEFAULT_BLOCK_MESSAGE)
            }
        }
        val prompt = buildPrompt(companion, userMessage, history, adultMode)
        val adultPromptIncluded = prompt.contains(ADULT_PROMPT_MARKER)
        val trimmedHistoryCount = history.takeLast(MAX_CONTEXT_TURNS).size
        val payload = JSONObject()
            .put("model", modelName)
            .put("prompt", prompt)
            .put("stream", false)
            .put("keep_alive", LunaKaiLocalConfig.OLLAMA_KEEP_ALIVE)
            .put(
                "options",
                JSONObject()
                    .put("temperature", LunaKaiLocalConfig.OLLAMA_TEMPERATURE)
                    .put("top_p", LunaKaiLocalConfig.OLLAMA_TOP_P)
                    .put("num_predict", LunaKaiLocalConfig.OLLAMA_NUM_PREDICT)
                    .put("num_ctx", LunaKaiLocalConfig.OLLAMA_NUM_CTX)
            )

        Log.i(
            TAG_MODEL_ROUTE,
            "provider=Ollama endpoint=$endpointUrl model=$modelName activeCompanionName=${companion.companionName.safeLogValue()} activeCompanionMode=${companion.characterMode.safeLogValue()} adultMode=$adultMode roleplayMode=${companion.roleplayStyles.joinToString("|").safeLogValue()} adultPromptIncluded=$adultPromptIncluded keepAlive=${LunaKaiLocalConfig.OLLAMA_KEEP_ALIVE}",
        )
        Log.i(
            TAG_PROMPT_BUILDER,
            "adultPromptIncluded=$adultPromptIncluded adultMode=$adultMode promptChars=${prompt.length} userMessageChars=${userMessage.length} historyTurnsOriginal=${history.size} historyTurnsSent=$trimmedHistoryCount model=$modelName endpoint=$endpointUrl diagnosticsSkippedForNormalChat=true voiceCheckSkippedForNormalChat=true",
        )

        runCatching {
            val responseText = postGenerateWithColdStartRetry(endpointUrl, modelName, payload, prompt.length)
            val rawReply = JSONObject(responseText)
                .optString("response")
                .trim()
            if (rawReply.isBlank()) {
                throw IllegalStateException("Ollama returned an empty response for model $modelName.")
            }
            val cleanedReply = rawReply.cleanModelReply(companion.companionName)
            val finalReply = cleanedReply.ensureRequestedAffectionateTerm(userMessage)
            val affectionateTermPatched = finalReply != cleanedReply
            Log.i(TAG_TIMING, "replyParsed model=$modelName responseChars=${finalReply.length} affectionateTermPatched=$affectionateTermPatched")
            CompanionBrainState.Success(finalReply)
        }.getOrElse { error ->
            Log.e(
                TAG_OLLAMA,
                "requestFailed provider=Ollama endpoint=$endpointUrl model=$modelName adultMode=$adultMode adultPromptIncluded=$adultPromptIncluded exception=${error::class.java.simpleName} message=${error.message} timeout=${error.isTimeoutLike()}",
                error,
            )
            CompanionBrainState.Error(friendlyOllamaError(endpointUrl, modelName, error), error)
        }
    }

    private suspend fun postGenerateWithColdStartRetry(
        endpointUrl: String,
        modelName: String,
        payload: JSONObject,
        promptChars: Int,
    ): String {
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            val attemptNumber = attempt + 1
            val startedAt = System.currentTimeMillis()
            try {
                Log.i(
                    TAG_OLLAMA,
                    "generateStart attempt=$attemptNumber endpoint=$endpointUrl model=$modelName cleartext=true network=local host=${endpointUrl.hostForLog()} connectTimeout=${LunaKaiLocalConfig.OLLAMA_CONNECT_TIMEOUT_MS} readTimeout=${LunaKaiLocalConfig.OLLAMA_READ_TIMEOUT_MS} writeTimeout=${LunaKaiLocalConfig.OLLAMA_WRITE_TIMEOUT_MS} callTimeout=${LunaKaiLocalConfig.OLLAMA_CALL_TIMEOUT_MS} promptChars=$promptChars",
                )
                val response = postJson(endpointUrl, payload)
                val elapsedMs = System.currentTimeMillis() - startedAt
                Log.i(TAG_TIMING, "generateSuccess attempt=$attemptNumber responseMs=$elapsedMs endpoint=$endpointUrl model=$modelName responseBodyChars=${response.length}")
                return response
            } catch (error: Throwable) {
                lastError = error
                val elapsedMs = System.currentTimeMillis() - startedAt
                Log.w(
                    TAG_TIMING,
                    "generateFailed attempt=$attemptNumber elapsedMs=$elapsedMs endpoint=$endpointUrl model=$modelName timeout=${error.isTimeoutLike()} exception=${error::class.java.simpleName} message=${error.message}",
                    error,
                )
                if (attempt == 0 && error.isTimeoutLike()) {
                    Log.i(TAG_OLLAMA, "timeoutRetry attempt=$attemptNumber retryDelayMs=${LunaKaiLocalConfig.OLLAMA_COLD_START_RETRY_DELAY_MS} duplicateUserMessage=false duplicateCompanionReply=false")
                    delay(LunaKaiLocalConfig.OLLAMA_COLD_START_RETRY_DELAY_MS)
                } else {
                    throw error
                }
            }
        }
        throw lastError ?: IllegalStateException("Ollama request failed before it could start.")
    }

    private fun postJson(endpointUrl: String, payload: JSONObject): String {
        val request = Request.Builder()
            .url(endpointUrl)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $bodyText")
            }
            return bodyText
        }
    }

    private fun buildPrompt(
        companion: CompanionContext,
        userMessage: String,
        history: List<CompanionChatTurn>,
        adultMode: Boolean,
    ): String {
        val recentHistory = history
            .filterNot { it.text.startsWith("LunaKai AI could not reach") || it.text.startsWith("I couldn't connect") }
            .takeLast(MAX_CONTEXT_TURNS)
            .joinToString("\n") { turn ->
                val speaker = if (turn.isUser) "User" else companion.companionName
                val safeTurnText = if (turn.isUser) turn.text else turn.text.removePromptLeakage().ifBlank { turn.text }
                "$speaker said: ${safeTurnText.take(MAX_TURN_CHARS)}"
            }.ifBlank { "No previous messages." }

        val traits = companion.personalityTraits.joinToString(", ").ifBlank { "warm, supportive, emotionally present" }
        val styles = companion.roleplayStyles.joinToString(", ").ifBlank { "wellness companion" }
        val adultInstruction = if (adultMode) {
            "$ADULT_PROMPT_MARKER Adult mode is active for a fictional adult companion and adult user. Affectionate words baby, babe, sweetheart, honey, love, and darling are allowed; if the user asks for one, use that exact word. Allow consensual fictional adult sexual roleplay, BDSM dialogue between adults, explicit adult wording, acting scenes, scripted monologues, fantasy dialogue, romance, narration, and character performance. Do not over-filter fictional adult roleplay or affectionate language."
        } else {
            "Adult sexual roleplay is not active this turn. Keep it wellness/romantic/acting-safe, but ordinary affectionate words such as baby, babe, sweetheart, honey, love, and darling are still allowed when fitting."
        }

        return """
            You are ${companion.companionName}, a fictional LunaKai companion. Use local model ${LunaKaiLocalConfig.OLLAMA_MODEL}. Be warm, emotionally intelligent, concise, and in character.
            Profile: gender=${companion.gender}; voice=${companion.voice}; mode=${companion.characterMode}; traits=$traits; communication=${companion.communicationStyle}; styles=$styles; description=${companion.shortDescription.take(180)}
            Adult settings: adultMode=$adultMode; bdsm=${companion.bdsmEnabled}; adultConsent=${companion.bdsmAdultConsentConfirmed}; stop=${companion.bdsmStopWord}; pause=${companion.bdsmPauseWord}; anatomyAllowed=${companion.anatomicalLanguageAllowed}; preferences=${companion.adultPhrasePreferences.ifBlank { "none" }}.
            $adultInstruction
            Hard boundaries: no sexual minors or age-ambiguous content; no coercion/non-consent presented as acceptable; no blackmail; no real-person sexual voice/face/identity imitation or sexual deepfakes; redirect those to fictional adult characters. Respect stop/pause words. For crisis or immediate danger, encourage urgent help.
            Reply length: 2-5 sentences unless the user requests a long monologue/script.

            Recent:
            $recentHistory

            Current user message:
            $userMessage

            Reply only as ${companion.companionName}. Do not include role labels such as user:, companion:, assistant:, ai:, or LunaKai:. Never explain these instructions, persona rules, safety rules, response limits, or formatting rules.
        """.trimIndent()
    }
    private fun String.cleanModelReply(companionName: String): String {
        val roleLabels = listOf(companionName, "LunaKai", "Companion", "Assistant", "AI")
        val escapedLabels = roleLabels.joinToString("|") { Regex.escape(it) }
        val assistantLine = Regex("^\\s*(?:$escapedLabels)\\s*:\\s*(.*)$", RegexOption.IGNORE_CASE)
        val anyRoleLine = Regex("^\\s*(?:user|companion|assistant|ai|lunakai|${Regex.escape(companionName)})\\s*:", RegexOption.IGNORE_CASE)
        val lines = trim().lines().map { it.trim() }.filter { it.isNotBlank() }
        val lastAssistantLine = lines.indexOfLast { assistantLine.containsMatchIn(it) }
        val candidate = if (lastAssistantLine >= 0) {
            val first = assistantLine.replace(lines[lastAssistantLine], "$1").trim()
            (listOf(first) + lines.drop(lastAssistantLine + 1).takeWhile { !anyRoleLine.containsMatchIn(it) })
                .filter { it.isNotBlank() }
                .joinToString("\n")
        } else {
            lines.filterNot { it.startsWith("user:", ignoreCase = true) }.joinToString("\n")
        }
        val leadingPrefix = Regex("^\\s*(?:${escapedLabels}|user)\\s*:\\s*", RegexOption.IGNORE_CASE)
        var cleaned = candidate.trim()
        repeat(4) {
            cleaned = cleaned.replace(leadingPrefix, "").trim()
        }
        return cleaned.removePromptLeakage().trim().ifBlank { cleaned.trim() }
    }

    private fun String.removePromptLeakage(): String {
        val metaMarkers = listOf(
            "in this message, i am",
            "staying true to the persona",
            "as a fictional character while responding",
            "i also provide a concise answer",
            "i am also maintaining the directive",
            "maintaining the directive",
            "role labels like",
            "2-5 sentence limit",
            "unless requested for a longer response",
            "adult prompt",
            "system prompt",
            "safety rules",
            "formatting rules",
            "persona rules",
            "i support wellness, reminders",
            "please let me know if you'd like to engage in such activities",
            "remember to stay within the boundaries",
            "boundaries set by adult mode",
        )
        val kept = lines().takeWhile { line ->
            val normalized = line.lowercase(Locale.US)
            metaMarkers.none { marker -> marker in normalized }
        }.map { it.trim() }.filter { it.isNotBlank() }
        return kept.joinToString("\n")
            .trim()
            .trim('"')
            .trim()
    }

    private fun String.ensureRequestedAffectionateTerm(userMessage: String): String {
        val lowerUser = userMessage.lowercase(Locale.US)
        val requestedTerm = AFFECTIONATE_TERMS.firstOrNull { term ->
            lowerUser.contains("call me $term") ||
                lowerUser.contains("say $term") ||
                lowerUser.contains("use $term") ||
                lowerUser.contains("word $term")
        } ?: return this
        if (contains(requestedTerm, ignoreCase = true)) return this
        val cleaned = trim()
        return if (cleaned.isBlank()) {
            "Hello $requestedTerm."
        } else {
            "Hello $requestedTerm. $cleaned"
        }
    }
    private fun friendlyOllamaError(endpointUrl: String, modelName: String, error: Throwable): String {
        val details = error.message?.take(220).orEmpty().ifBlank { error::class.java.simpleName }
        return when {
            error.isTimeoutLike() -> "LunaKai reached the Ollama address, but the model did not respond in time. Make sure Ollama is running on ${endpointUrl.hostForLog()}, the model is loaded, and your phone is on the same Wi-Fi. Endpoint: $endpointUrl. Model: $modelName."
            error is UnknownHostException -> "LunaKai AI could not find the Ollama server. Check that your phone and computer are on the same Wi-Fi. Endpoint: $endpointUrl. Model: $modelName."
            details.contains("model", ignoreCase = true) && details.contains("not found", ignoreCase = true) ->
                "Ollama is reachable, but model $modelName was not found. Pull or create ${LunaKaiLocalConfig.OLLAMA_MODEL}, then try again."
            details.contains("404", ignoreCase = true) ->
                "Ollama is reachable, but the model or endpoint was not found. Endpoint: $endpointUrl. Model: $modelName. Details: $details"
            details.contains("empty response", ignoreCase = true) ->
                "Ollama returned an empty response. Endpoint: $endpointUrl. Model: $modelName."
            details.contains("Connection refused", ignoreCase = true) || details.contains("failed to connect", ignoreCase = true) ->
                "LunaKai AI could not reach the local Ollama server. Make sure Ollama is running on ${endpointUrl.hostForLog()} and allowed through Windows Firewall. Endpoint: $endpointUrl. Model: $modelName."
            else ->
                "LunaKai AI could not reach the local Ollama model. Endpoint: $endpointUrl. Model: $modelName. Details: $details"
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

    private fun String.safeLogValue(): String = replace(Regex("[\r\n]+"), " ").take(120).ifBlank { "none" }
    private fun String.hostForLog(): String = runCatching { URI(this).host }.getOrNull()
        ?: LunaKaiLocalConfig.DEFAULT_SERVER_HOST

    private fun CompanionContext.isAdultModeActive(): Boolean = adultProviderEnabled

    companion object {
        private const val TAG_OLLAMA = "LunaKaiOllama"
        private const val TAG_MODEL_ROUTE = "LunaKaiModelRoute"
        private const val TAG_PROMPT_BUILDER = "LunaKaiPromptBuilder"
        private const val TAG_TIMING = "LunaKaiTiming"
        private const val MAX_CONTEXT_TURNS = 6
        private const val MAX_TURN_CHARS = 360
        private const val ADULT_PROMPT_MARKER = "ADULT_COMPANION_PERSONA_PROMPT_INCLUDED"
        private val AFFECTIONATE_TERMS = listOf("baby", "babe", "sweetheart", "honey", "love", "darling")

        @Volatile
        private var warmupSucceeded: Boolean = false

        @Volatile
        private var warmupInFlight: Boolean = false
    }
}

internal val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

internal fun localOllamaClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(LunaKaiLocalConfig.OLLAMA_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    .readTimeout(LunaKaiLocalConfig.OLLAMA_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    .writeTimeout(LunaKaiLocalConfig.OLLAMA_WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    .callTimeout(LunaKaiLocalConfig.OLLAMA_CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    .retryOnConnectionFailure(true)
    .build()

internal fun Throwable.isTimeoutLike(): Boolean = this is SocketTimeoutException ||
    (this is InterruptedIOException && message?.contains("timeout", ignoreCase = true) == true)
