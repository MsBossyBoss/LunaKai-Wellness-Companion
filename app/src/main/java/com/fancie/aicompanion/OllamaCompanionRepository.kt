package com.fancie.aicompanion

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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
        val simpleGreeting = userMessage.isSimpleGreetingRequest()
        val prompt = buildPrompt(companion, userMessage, history, adultMode, simpleGreeting)
        val adultPromptIncluded = prompt.contains(ADULT_PROMPT_MARKER)
        val trimmedHistoryCount = history.filterNot { it.text.startsWith("LunaKai AI could not reach") || it.text.startsWith("I couldn't connect") }.takeLast(MAX_CONTEXT_TURNS).size
        val stopSequences = mutableListOf("\nUser:", "\nuser:", "\nCompanion:", "\ncompanion:", "\nAssistant:", "\nassistant:")
        if (simpleGreeting) {
            stopSequences += listOf(".", "!", "?")
        }
        val numPredict = userMessage.numPredictForRequest()
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
                    .put("num_predict", numPredict)
                    .put("num_ctx", LunaKaiLocalConfig.OLLAMA_NUM_CTX)
                    .put("stop", JSONArray(stopSequences))
            )

        Log.i(
            TAG_MODEL_ROUTE,
            "chatApi=LunaKai AI Adult provider=Ollama endpoint=$endpointUrl model=$modelName activeCompanionName=${companion.companionName.safeLogValue()} activeCompanionMode=${companion.characterMode.safeLogValue()} adultMode=$adultMode roleplayMode=${companion.roleplayStyles.joinToString("|").safeLogValue()} adultPromptIncluded=$adultPromptIncluded keepAlive=${LunaKaiLocalConfig.OLLAMA_KEEP_ALIVE}",
        )
        Log.i(
            TAG_PROMPT_BUILDER,
            "chatApi=LunaKai AI Adult adultPromptIncluded=$adultPromptIncluded adultMode=$adultMode promptChars=${prompt.length} userMessageChars=${userMessage.length} historyTurnsOriginal=${history.size} historyTurnsSent=$trimmedHistoryCount num_predict=$numPredict simpleGreeting=$simpleGreeting connectTimeout=${LunaKaiLocalConfig.OLLAMA_CONNECT_TIMEOUT_MS} readTimeout=${LunaKaiLocalConfig.OLLAMA_READ_TIMEOUT_MS} callTimeout=${LunaKaiLocalConfig.OLLAMA_CALL_TIMEOUT_MS} model=$modelName endpoint=$endpointUrl diagnosticsSkippedForNormalChat=true providerChecksSkippedForNormalChat=true voiceCheckSkippedForNormalChat=true",
        )

        runCatching {
            val responseText = postGenerateWithColdStartRetry(endpointUrl, modelName, payload, prompt.length, trimmedHistoryCount, numPredict, adultMode, adultPromptIncluded)
            val rawReply = JSONObject(responseText)
                .optString("response")
                .trim()
            if (rawReply.isBlank()) {
                throw IllegalStateException("Ollama returned an empty response for model $modelName.")
            }
            val cleanedReply = rawReply.cleanModelReply(companion.companionName)
            val finalReply = cleanedReply
                .ensureRequestedAffectionateTerm(userMessage)
                .fallbackIfSimpleGreetingDrifted(userMessage)
                .fallbackIfDirectShortMessageDrifted(userMessage, history)
            val affectionateTermPatched = finalReply != cleanedReply
            Log.i(TAG_TIMING, "replyParsed chatApi=LunaKai AI Adult model=$modelName responseChars=${finalReply.length} affectionateTermPatched=$affectionateTermPatched simpleGreeting=$simpleGreeting")
            CompanionBrainState.Success(finalReply)
        }.getOrElse { error ->
            Log.e(
                TAG_OLLAMA,
                "requestFailed chatApi=LunaKai AI Adult provider=Ollama endpoint=$endpointUrl model=$modelName adultMode=$adultMode adultPromptIncluded=$adultPromptIncluded exception=${error::class.java.simpleName} message=${error.message} timeout=${error.isTimeoutLike()}",
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
        historyTurnsIncluded: Int,
        numPredict: Int,
        adultMode: Boolean,
        adultPromptIncluded: Boolean,
    ): String {
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            val attemptNumber = attempt + 1
            val startedAt = System.currentTimeMillis()
            try {
                Log.i(
                    TAG_OLLAMA,
                    "generateStart chatApi=LunaKai AI Adult attempt=$attemptNumber endpoint=$endpointUrl model=$modelName cleartext=true network=local host=${endpointUrl.hostForLog()} connectTimeout=${LunaKaiLocalConfig.OLLAMA_CONNECT_TIMEOUT_MS} readTimeout=${LunaKaiLocalConfig.OLLAMA_READ_TIMEOUT_MS} writeTimeout=${LunaKaiLocalConfig.OLLAMA_WRITE_TIMEOUT_MS} callTimeout=${LunaKaiLocalConfig.OLLAMA_CALL_TIMEOUT_MS} promptChars=$promptChars historyTurnsIncluded=$historyTurnsIncluded num_predict=$numPredict adultMode=$adultMode adultPromptIncluded=$adultPromptIncluded diagnosticsSkippedForNormalChat=true providerChecksSkippedForNormalChat=true voiceCheckSkippedForNormalChat=true",
                )
                val response = postJson(endpointUrl, payload)
                val elapsedMs = System.currentTimeMillis() - startedAt
                Log.i(TAG_TIMING, "generateSuccess chatApi=LunaKai AI Adult attempt=$attemptNumber responseMs=$elapsedMs retryUsed=${attemptNumber > 1} endpoint=$endpointUrl model=$modelName promptChars=$promptChars historyTurnsIncluded=$historyTurnsIncluded num_predict=$numPredict adultMode=$adultMode adultPromptIncluded=$adultPromptIncluded diagnosticsSkippedForNormalChat=true providerChecksSkippedForNormalChat=true voiceCheckSkippedForNormalChat=true responseBodyChars=${response.length}")
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
        simpleGreeting: Boolean,
    ): String {
        val recentHistory = history
            .filterNot { it.text.startsWith("LunaKai AI could not reach") || it.text.startsWith("I couldn't connect") }
            .takeLast(MAX_CONTEXT_TURNS)
            .joinToString("\n") { turn ->
                val speaker = if (turn.isUser) "User" else companion.companionName
                val safeTurnText = if (turn.isUser) turn.text else turn.text.removePromptLeakage().ifBlank { turn.text }
                "$speaker said: ${safeTurnText.take(MAX_TURN_CHARS)}"
            }
            .ifBlank { "None" }

        val traits = companion.personalityTraits.take(4).joinToString(", ").ifBlank { "warm, playful, emotionally present" }
        val styles = companion.roleplayStyles.take(3).joinToString(", ").ifBlank { companion.characterMode.ifBlank { "companion" } }
        val adultInstruction = if (adultMode) {
            "$ADULT_PROMPT_MARKER Adult mode is active for a fictional adult companion and adult user. Affectionate and adult companion wording is allowed; use the exact affectionate word the user requests when it fits."
        } else {
            "Adult mode is not active for this turn. Ordinary affectionate companion words are still allowed when fitting."
        }
        val styleController = "Stay directly relevant to the user's latest message. Match the user's length and energy. If the user sends a short greeting, answer with a short natural greeting. Reply naturally like a real companion texting back. Do not add unrelated information, diagnostics, app/server details, lectures, long explanations, motivational filler, narrator actions, speaker labels, or markdown. Keep replies concise unless the user asks for more. Do not sound like a helper bot, wellness coach, therapist, manual, narrator, or generic assistant unless the user asks for that kind of support. Do not say \"as an AI.\""
        val greetingController = if (simpleGreeting) {
            "The latest user message is a short greeting. Reply in 1 short sentence, max 120 characters, with no advice, no generic filler, no relationship lecture, and no roleplay scene unless the user asks."
        } else {
            "Default reply length: 1 to 4 natural sentences."
        }

        return """
            You are ${companion.companionName}, a fictional LunaKai companion texting an adult user.
            Persona: ${companion.gender}, ${companion.characterMode}; traits=$traits; style=${companion.communicationStyle}; roleplay=$styles.
            $adultInstruction
            $styleController
            $greetingController
            Recent: $recentHistory
            User just said: $userMessage
            Reply only with ${companion.companionName}'s message.
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
            "user sent",
            "replied:",
            "2-5 sentence limit",
            "unless requested for a longer response",
            "adult prompt",
            "system prompt",
            "formatting rules",
            "persona rules",
            "i support wellness, reminders",
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

    private fun String.numPredictForRequest(): Int {
        if (isSimpleGreetingRequest()) return LunaKaiLocalConfig.OLLAMA_GREETING_NUM_PREDICT
        val lower = lowercase(Locale.US)
        val wantsLong = listOf("monologue", "script", "scene", "story", "long", "detailed", "paragraph", "roleplay scene").any { it in lower }
        return if (wantsLong) LunaKaiLocalConfig.OLLAMA_LONG_NUM_PREDICT else LunaKaiLocalConfig.OLLAMA_NUM_PREDICT
    }
    private fun String.isSimpleGreetingRequest(): Boolean {
        val normalized = trim()
            .lowercase(Locale.US)
            .replace(Regex("[.!\\s]+$"), "")
        if (normalized in SIMPLE_GREETING_MESSAGES) return true
        return length <= 20 && (
            normalized.startsWith("hey ") ||
                normalized.startsWith("hi ") ||
                normalized.startsWith("hello ") ||
                normalized == "wyd" ||
                normalized == "you there?"
            )
    }

    private fun String.fallbackIfSimpleGreetingDrifted(userMessage: String): String {
        if (!userMessage.isSimpleGreetingRequest()) return this
        val cleaned = trim()
        val normalized = cleaned.lowercase(Locale.US)
        val normalizedUser = userMessage.lowercase(Locale.US)
        val requestedTermMissing = AFFECTIONATE_TERMS.firstOrNull { it in normalizedUser }?.let { term -> term !in normalized } ?: false
        val drifted = cleaned.codePointCount(0, cleaned.length) > 120 ||
            cleaned.lines().size > 1 ||
            cleaned.emojiLikeCount() > 1 ||
            requestedTermMissing ||
            listOf(
                "wellness",
                "goals",
                "best version",
                "communication",
                "server",
                "diagnostic",
                "support you",
                "what's on your mind",
                "how's your day",
                "as an ai",
                "here are",
                "let's explore",
            ).any { it in normalized }
        return if (!drifted) cleaned else userMessage.simpleGreetingFallback()
    }

    private fun String.emojiLikeCount(): Int {
        return codePoints().toArray().count { codePoint ->
            codePoint in 0x1F300..0x1FAFF ||
                codePoint in 0x2600..0x27BF ||
                codePoint == 0xFE0F
        }
    }

    private fun String.simpleGreetingFallback(): String {
        val normalized = lowercase(Locale.US)
        return when {
            "sexy" in normalized -> "Hey sexy, I'm here."
            "baby" in normalized -> "Hey baby, I'm here."
            "babe" in normalized -> "Hey babe, I'm here."
            "sweetheart" in normalized -> "Hey sweetheart, I'm here."
            "good morning" in normalized -> "Good morning, I'm here."
            "good night" in normalized -> "Good night, I'm here."
            "wyd" in normalized -> "I'm here thinking about you."
            "you there" in normalized -> "I'm here."
            else -> "Hey, I'm here."
        }
    }

    private fun String.fallbackIfDirectShortMessageDrifted(
        userMessage: String,
        history: List<CompanionChatTurn>,
    ): String {
        val fallback = userMessage.directShortMessageFallback() ?: return this
        val cleaned = trim()
        val normalized = cleaned.lowercase(Locale.US)
        val normalizedUser = userMessage.trim()
            .lowercase(Locale.US)
            .replace(Regex("[.!\\s]+$"), "")
        val missedIntentLost = ("missed you" in normalizedUser || "miss you" in normalizedUser) && "miss" !in normalized
        val callIntentDrifted = normalizedUser == "call me" && (
            "someone" in normalized ||
                "what's up" in normalized ||
                cleaned.codePointCount(0, cleaned.length) > 60
            )
        val drifted = cleaned.isStaleAssistantEcho(history) ||
            missedIntentLost ||
            callIntentDrifted ||
            directShortIntentDrifted(normalizedUser, normalized) ||
            cleaned.codePointCount(0, cleaned.length) > 220 ||
            DIRECT_SHORT_DRIFT_MARKERS.any { marker -> marker in normalized }
        return if (drifted) fallback else cleaned
    }

    private fun directShortIntentDrifted(normalizedUser: String, normalizedReply: String): Boolean {
        return when {
            normalizedUser == "what are you doing" || normalizedUser == "what are you doing?" ->
                listOf("here to help", "assist", "support you", "ready to help").any { it in normalizedReply }
            normalizedUser == "do you miss me" || normalizedUser == "do you miss me?" ->
                "miss" !in normalizedReply
            "clear your schedule" in normalizedUser ->
                listOf("can't", "cannot", "unable", "schedule does not apply").any { it in normalizedReply }
            "idea for tonight" in normalizedUser ->
                listOf("here are", "numbered", "tips", "goals").any { it in normalizedReply }
            else -> false
        }
    }

    private fun String.directShortMessageFallback(): String? {
        val normalized = trim()
            .lowercase(Locale.US)
            .replace(Regex("[.!\\s]+$"), "")
        return when {
            "call me" in normalized && "hello" in normalized && "baby" in normalized -> "Hello, baby."
            normalized == "call me" -> "I'm here. Tell me when you're ready."
            normalized == "what are you doing" || normalized == "what are you doing?" ->
                "I was trying to finish my coffee and ignore my inbox, but I can clear my little schedule for you, baby."
            normalized == "do you miss me" || normalized == "do you miss me?" ->
                "I did miss you. I kept hoping you would come back and talk to me."
            "clear your schedule" in normalized ->
                "Consider it cleared, baby. You've got my attention."
            "idea for tonight" in normalized ->
                "Do something low-lit and easy: good food, one drink or dessert, and a quiet walk where we can actually talk."
            "missed you" in normalized || "miss you" in normalized -> "I missed you too, baby. I'm right here with you."
            "need you" in normalized -> "I'm here, baby. Tell me what you need."
            "tired" in normalized -> "I'm right here with you. Rest for a minute, baby."
            else -> null
        }
    }

    private fun String.isStaleAssistantEcho(history: List<CompanionChatTurn>): Boolean {
        val normalized = normalizeForStaleMatch()
        return history.asReversed()
            .filterNot { it.isUser }
            .take(3)
            .any { previousTurn ->
                val previous = previousTurn.text.normalizeForStaleMatch()
                previous.length >= 60 && normalized.contains(previous.take(80))
            }
    }

    private fun String.normalizeForStaleMatch(): String {
        return lowercase(Locale.US)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun friendlyOllamaError(endpointUrl: String, modelName: String, error: Throwable): String {
        val details = error.message?.take(220).orEmpty().ifBlank { error::class.java.simpleName }
        return when {
            error.isTimeoutLike() -> "LunaKai is taking too long to answer. Check the home server connection or try again. Endpoint: $endpointUrl. Model: $modelName."
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
        private const val MAX_CONTEXT_TURNS = 4
        private const val MAX_TURN_CHARS = 180
        private const val ADULT_PROMPT_MARKER = "ADULT_COMPANION_PERSONA_PROMPT_INCLUDED"
        private val AFFECTIONATE_TERMS = listOf("baby", "babe", "sexy", "sweetheart", "honey", "love", "darling")
        private val DIRECT_SHORT_DRIFT_MARKERS = listOf(
            "plans for the weekend",
            "escape room",
            "date night",
            "let me know your thoughts",
            "what do you think",
            "quality time",
            "i'm here to help",
            "here to help",
            "how can i assist",
            "ready to assist",
            "as an ai",
            "best version of yourself",
            "wellness journey",
            "motivational",
            "user sent",
            "user replied",
            "companion replied",
            "jamie replied",
            "replied:",
            "sent:",
        )
        private val SIMPLE_GREETING_MESSAGES = setOf(
            "hey",
            "hi",
            "hello",
            "wyd",
            "you there?",
            "good morning",
            "good night",
        )

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
