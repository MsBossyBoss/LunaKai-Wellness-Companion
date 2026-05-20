package com.fancie.aicompanion

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiCompanionBrainProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build(),
) : CompanionBrainProvider {
    override suspend fun generateReply(
        companion: CompanionContext,
        userMessage: String,
        history: List<CompanionChatTurn>,
    ): CompanionBrainState = withContext(Dispatchers.IO) {
        val apiKey = companion.geminiApiKey.trim()
        val model = companion.geminiModel.trim().ifBlank { LunaKaiLocalConfig.GEMINI_DEFAULT_MODEL }
        if (apiKey.isBlank()) {
            Log.i(
                "LunaKaiGeminiLive",
                "geminiSetupNeeded liveApi=Gemini geminiModel=${model.safeGeminiLog()} apiKeySet=false normalChatProviderUsed=false lunaKaiAiAdultUsed=false androidTtsUsed=false androidSpeechUsed=false",
            )
            return@withContext CompanionBrainState.Error("Gemini setup needed: add a Gemini API key in Settings > AI Provider Settings before Live Companion text can answer.")
        }

        val prompt = buildLivePrompt(companion, userMessage, history)
        val url = "${LunaKaiLocalConfig.GEMINI_API_BASE_URL}/${model}:generateContent"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("key", apiKey)
            .build()
        val payload = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", prompt))),
                ),
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.8)
                    .put("topP", 0.9)
                    .put("maxOutputTokens", 180),
            )
        val startedAt = System.currentTimeMillis()
        Log.i(
            "LunaKaiGeminiLive",
            "geminiRequestStart liveApi=Gemini geminiModel=${model.safeGeminiLog()} endpoint=${url.redactGeminiKey()} promptChars=${prompt.length} historyTurnsIncluded=${history.takeLast(6).size} apiKeySet=true normalChatProviderUsed=false lunaKaiAiAdultUsed=false androidTtsUsed=false androidSpeechUsed=false",
        )
        runCatching {
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val elapsedMs = System.currentTimeMillis() - startedAt
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${body.take(500)}")
                val reply = parseGeminiText(body).cleanLiveReply(companion.companionName)
                if (reply.isBlank()) throw IOException("Gemini returned an empty live reply.")
                Log.i(
                    "LunaKaiGeminiLive",
                    "geminiSuccess liveApi=Gemini geminiModel=${model.safeGeminiLog()} geminiResponseMs=$elapsedMs responseChars=${reply.length} historyTurnsIncluded=${history.takeLast(6).size} normalChatProviderUsed=false lunaKaiAiAdultUsed=false androidTtsUsed=false androidSpeechUsed=false",
                )
                CompanionBrainState.Success(reply)
            }
        }.getOrElse { error ->
            Log.w(
                "LunaKaiGeminiLive",
                "geminiFailed liveApi=Gemini geminiModel=${model.safeGeminiLog()} geminiResponseMs=${System.currentTimeMillis() - startedAt} exception=${error::class.java.simpleName} message=${error.message} normalChatProviderUsed=false lunaKaiAiAdultUsed=false androidTtsUsed=false androidSpeechUsed=false",
                error,
            )
            CompanionBrainState.Error("Gemini Live Companion could not answer yet: ${error.message ?: error::class.java.simpleName}", error)
        }
    }

    private fun buildLivePrompt(
        companion: CompanionContext,
        userMessage: String,
        history: List<CompanionChatTurn>,
    ): String {
        val recent = history.takeLast(6).joinToString("\n") { turn ->
            val speaker = if (turn.isUser) "User" else companion.companionName
            "$speaker: ${turn.text.take(260)}"
        }.ifBlank { "None" }
        return """
            You are ${companion.companionName} in a LunaKai Live Companion call.
            Reply naturally in first person as the live companion.
            Keep the reply direct and conversational unless the user asks for detail.
            Recent live conversation:
            $recent
            User: $userMessage
            ${companion.companionName}:
        """.trimIndent()
    }

    private fun parseGeminiText(body: String): String {
        val root = JSONObject(body)
        val parts = root.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
        return (0 until (parts?.length() ?: 0))
            .joinToString("\n") { index -> parts?.optJSONObject(index)?.optString("text").orEmpty() }
            .trim()
    }
}

private fun String.cleanLiveReply(companionName: String): String {
    val labels = listOf(companionName, "assistant", "companion", "ai", "lunakai", "user")
        .joinToString("|") { Regex.escape(it) }
    return trim()
        .replace(Regex("(?im)^\\s*(?:$labels)\\s*:\\s*"), "")
        .replace(Regex("\\*[^*]{1,180}\\*"), " ")
        .replace(Regex("`{1,3}[^`]*`{1,3}"), " ")
        .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), " ")
        .replace(Regex("\\[[^]]*]\\([^)]*\\)"), " ")
        .replace(Regex("[*_#>~]+"), " ")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}

private fun String.safeGeminiLog(): String = replace(Regex("[\\r\\n]+"), " ").take(180)

private fun okhttp3.HttpUrl.redactGeminiKey(): String = newBuilder()
    .setQueryParameter("key", "***")
    .build()
    .toString()
