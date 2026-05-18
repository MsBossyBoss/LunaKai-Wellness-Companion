package com.fancie.aicompanion

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.UnknownHostException

private const val DIAGNOSTIC_TAG = "LunaKaiOllamaDiagnostic"
private const val DIAGNOSTIC_PROMPT = "You are LunaKai, a warm fictional adult companion. Greet me affectionately and call me baby once."

data class OllamaDiagnosticStep(
    val name: String,
    val url: String,
    val ok: Boolean,
    val elapsedMs: Long,
    val detail: String,
    val exceptionClass: String? = null,
)

data class OllamaDiagnosticReport(
    val steps: List<OllamaDiagnosticStep>,
) {
    val serverReachable: Boolean get() = steps.firstOrNull { it.name == "server" }?.ok == true
    val tagsReachable: Boolean get() = steps.firstOrNull { it.name == "tags" }?.ok == true
    val modelFound: Boolean get() = steps.firstOrNull { it.name == "tags" }?.detail?.contains("modelFound=true") == true
    val generateSucceeded: Boolean get() = steps.firstOrNull { it.name == "generate" }?.ok == true
    val generateCanSayBaby: Boolean get() = steps.firstOrNull { it.name == "generate" }?.detail?.contains("containsBaby=true") == true

    val appTextChatRouteSucceeded: Boolean get() = steps.firstOrNull { it.name == "appTextChatRoute" }?.ok == true
    val appTextChatRouteCanSayBaby: Boolean get() = steps.firstOrNull { it.name == "appTextChatRoute" }?.detail?.contains("containsBaby=true") == true
    fun toUserSummary(): String = steps.joinToString("\n") { step ->
        val status = if (step.ok) "OK" else "FAILED"
        "$status ${step.name} ${step.elapsedMs}ms - ${step.detail}"
    }

    fun toLogString(): String = buildString {
        append("serverReachable=$serverReachable tagsReachable=$tagsReachable modelFound=$modelFound generateSucceeded=$generateSucceeded generateCanSayBaby=$generateCanSayBaby appTextChatRouteSucceeded=$appTextChatRouteSucceeded appTextChatRouteCanSayBaby=$appTextChatRouteCanSayBaby base=${LunaKaiLocalConfig.OLLAMA_BASE_URL} model=${LunaKaiLocalConfig.OLLAMA_MODEL}")
        steps.forEach { step ->
            append(" | ${step.name}: ok=${step.ok} elapsedMs=${step.elapsedMs} url=${step.url} detail=${step.detail} exception=${step.exceptionClass.orEmpty()}")
        }
    }
}

object LunaKaiOllamaDiagnostic {
    private val client: OkHttpClient = localOllamaClient()

    suspend fun run(): OllamaDiagnosticReport = withContext(Dispatchers.IO) {
        Log.i(
            "LunaKaiLocalConfig",
            "Diagnostic cleartext local network host=${LunaKaiLocalConfig.OLLAMA_HOST} base=${LunaKaiLocalConfig.OLLAMA_BASE_URL} generate=${LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT} model=${LunaKaiLocalConfig.OLLAMA_MODEL} connectTimeout=${LunaKaiLocalConfig.OLLAMA_CONNECT_TIMEOUT_MS} readTimeout=${LunaKaiLocalConfig.OLLAMA_READ_TIMEOUT_MS} writeTimeout=${LunaKaiLocalConfig.OLLAMA_WRITE_TIMEOUT_MS} callTimeout=${LunaKaiLocalConfig.OLLAMA_CALL_TIMEOUT_MS} keepAlive=${LunaKaiLocalConfig.OLLAMA_KEEP_ALIVE}",
        )
        Log.i(
            "LunaKaiModelRoute",
            "diagnostic provider=Ollama endpoint=${LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT} model=${LunaKaiLocalConfig.OLLAMA_MODEL} adultMode=true adultPromptIncluded=true activeCompanionName=LunaKai activeCompanionMode=adult_companion",
        )
        Log.i(
            "LunaKaiPromptBuilder",
            "diagnostic adultPromptIncluded=true adultMode=true promptChars=${DIAGNOSTIC_PROMPT.length} privateUserContentLogged=false",
        )
        val steps = listOf(
            runStep("server", LunaKaiLocalConfig.OLLAMA_BASE_URL) {
                val body = getString(LunaKaiLocalConfig.OLLAMA_BASE_URL)
                val saysRunning = body.contains("Ollama is running", ignoreCase = true)
                if (!saysRunning) throw IOException("Unexpected root response: ${body.take(120)}")
                "server reachable; root says Ollama is running; cleartext=true; network=local"
            },
            runStep("tags", LunaKaiLocalConfig.OLLAMA_TAGS_ENDPOINT) {
                val body = getString(LunaKaiLocalConfig.OLLAMA_TAGS_ENDPOINT)
                val root = JSONObject(body)
                val models = root.optJSONArray("models")
                val names = (0 until (models?.length() ?: 0)).mapNotNull { index ->
                    models?.optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }
                }
                val modelFound = LunaKaiLocalConfig.OLLAMA_MODEL in names
                if (!modelFound) throw IOException("model not found: ${LunaKaiLocalConfig.OLLAMA_MODEL}; available=${names.joinToString(", ").take(400)}")
                "tags reachable; modelFound=true; models=${names.joinToString(", ").take(400)}"
            },
            runStep("generate", LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT) {
                val payload = JSONObject()
                    .put("model", LunaKaiLocalConfig.OLLAMA_MODEL)
                    .put("prompt", DIAGNOSTIC_PROMPT)
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
                val startedAt = System.currentTimeMillis()
                val body = postString(LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT, payload)
                Log.i("LunaKaiTiming", "diagnosticGenerateBodyReceived elapsedMs=${System.currentTimeMillis() - startedAt} model=${LunaKaiLocalConfig.OLLAMA_MODEL} endpoint=${LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT}")
                val reply = JSONObject(body).optString("response").trim()
                if (reply.isBlank()) throw IOException("empty generate response")
                val containsBaby = reply.contains("baby", ignoreCase = true)
                "generate succeeded; containsBaby=$containsBaby; responseChars=${reply.length}; replyPreview=${reply.take(120)}"
            },
            runStep("appTextChatRoute", LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT) {
                val companion = CompanionContext(
                    companionId = "diagnostic_lunakai_adult",
                    companionName = "LunaKai",
                    gender = "Female",
                    voice = "Soft Female",
                    characterMode = "adult companion mode",
                    personalityTraits = listOf("warm", "affectionate", "romantic", "playful"),
                    communicationStyle = "affectionate adult companion",
                    supportFocus = listOf("adult companion conversation", "romantic connection"),
                    shortDescription = "Diagnostic fictional adult companion context.",
                    roleplayStyles = listOf("romantic companion mode", "adult roleplay mode", "BDSM roleplay mode", "acting/monologue mode"),
                    bdsmEnabled = true,
                    bdsmAdultConsentConfirmed = true,
                    anatomicalLanguageAllowed = true,
                    adultPhrasePreferences = "baby, babe, sweetheart, honey, love, darling",
                    adultProviderEnabled = true,
                    adultProviderEndpoint = LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT,
                    adultProviderModel = LunaKaiLocalConfig.OLLAMA_MODEL,
                    adminEmoIntelProfile = "- Affectionate: 95/100\n- Romantic: 90/100",
                )
                val startedAt = System.currentTimeMillis()
                val result = AdultRoleplayRepository().sendMessage(companion, "Call me baby and say hello.", emptyList())
                val elapsedMs = System.currentTimeMillis() - startedAt
                when (result) {
                    CompanionBrainState.Loading -> throw IOException("unexpected loading state")
                    is CompanionBrainState.Error -> throw IOException(result.message)
                    is CompanionBrainState.Success -> {
                        val reply = result.text.trim()
                        val containsBaby = reply.contains("baby", ignoreCase = true)
                        Log.i("LunaKaiTiming", "diagnosticAppTextChatRoute elapsedMs=$elapsedMs model=${LunaKaiLocalConfig.OLLAMA_MODEL} endpoint=${LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT} containsBaby=$containsBaby")
                        "repository text chat route succeeded; adultMode=true; adultPromptIncluded=true; containsBaby=$containsBaby; responseChars=${reply.length}; replyPreview=${reply.take(120)}"
                    }
                }
            },
        )
        val report = OllamaDiagnosticReport(steps)
        Log.i(DIAGNOSTIC_TAG, report.toLogString())
        report
    }

    private suspend fun runStep(name: String, url: String, block: suspend () -> String): OllamaDiagnosticStep {
        val startedAt = System.currentTimeMillis()
        return try {
            val detail = block()
            val elapsedMs = System.currentTimeMillis() - startedAt
            Log.i(DIAGNOSTIC_TAG, "LunaKai $name OK elapsedMs=$elapsedMs url=$url detail=$detail")
            OllamaDiagnosticStep(name, url, ok = true, elapsedMs = elapsedMs, detail = detail)
        } catch (error: Throwable) {
            val elapsedMs = System.currentTimeMillis() - startedAt
            val detail = diagnosticMessage(error)
            Log.e(DIAGNOSTIC_TAG, "LunaKai $name FAILED elapsedMs=$elapsedMs url=$url cleartext=true network=local timeout=${error.isTimeoutLike()} connection refused=${detail.contains("refused", ignoreCase = true)} model not found=${detail.contains("model not found", ignoreCase = true)} UnknownHost=${error is UnknownHostException} IOException=${error is IOException} HttpException=false SocketTimeout=${error.isTimeoutLike()} detail=$detail", error)
            OllamaDiagnosticStep(name, url, ok = false, elapsedMs = elapsedMs, detail = detail, exceptionClass = error::class.java.simpleName)
        }
    }

    private fun getString(url: String): String {
        val request = Request.Builder().url(url).get().build()
        return execute(request)
    }

    private fun postString(url: String, payload: JSONObject): String {
        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return execute(request)
    }

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${body.take(500)}")
            return body
        }
    }

    private fun diagnosticMessage(error: Throwable): String {
        val details = error.message.orEmpty().ifBlank { error::class.java.simpleName }
        return when {
            error.isTimeoutLike() -> "timeout: LunaKai reached ${LunaKaiLocalConfig.OLLAMA_HOST} but Ollama/model did not respond before timeout. Check model cold start, Ollama running, Wi-Fi, and Windows Firewall. $details"
            error is UnknownHostException -> "UnknownHost: phone could not resolve local Ollama host ${LunaKaiLocalConfig.OLLAMA_HOST}. $details"
            details.contains("refused", ignoreCase = true) -> "connection refused: Ollama may not be listening on LAN port ${LunaKaiLocalConfig.OLLAMA_PORT}. $details"
            details.contains("model not found", ignoreCase = true) -> "model not found: ${LunaKaiLocalConfig.OLLAMA_MODEL}. $details"
            details.contains("HTTP", ignoreCase = true) -> "HttpException: $details"
            error is IOException -> "IOException: $details"
            else -> details
        }
    }
}
