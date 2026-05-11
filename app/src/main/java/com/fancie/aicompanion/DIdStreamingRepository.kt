package com.fancie.aicompanion

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed interface DIdStreamState {
    data object Idle : DIdStreamState
    data object Connecting : DIdStreamState
    data class Connected(val message: String) : DIdStreamState
    data class Error(val message: String) : DIdStreamState
}

class DIdStreamingRepository(
    private val apiKey: String,
    private val appContext: Context,
) {
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val authHeader: String
        get() = "Basic ${Base64.encodeToString("$apiKey:".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}"

    private var streamId: String? = null
    private var sessionId: String? = null

    var onStateChange: ((DIdStreamState) -> Unit)? = null

    suspend fun startStream(imageSourceUrl: String): DIdStreamState = withContext(Dispatchers.IO) {
        try {
            onStateChange?.invoke(DIdStreamState.Connecting)

            val bodyJson = if (imageSourceUrl.startsWith("presenter:")) {
                JSONObject().apply {
                    put("presenter_id", imageSourceUrl.removePrefix("presenter:").trim())
                    put("config", JSONObject().apply { put("fluent", true); put("stitch", true) })
                }
            } else {
                JSONObject().apply {
                    put("source_url", imageSourceUrl.trim())
                    put("config", JSONObject().apply { put("fluent", true); put("stitch", true) })
                }
            }

            val resp = postJson("https://api.d-id.com/talks/streams", bodyJson.toString())
            streamId = resp.getString("id")
            sessionId = resp.getString("session_id")
            onStateChange?.invoke(DIdStreamState.Connected("Avatar stream ready."))
            DIdStreamState.Connected("Avatar stream ready.")
        } catch (e: Exception) {
            Log.e(TAG, "startStream failed", e)
            val err = friendlyError(e)
            onStateChange?.invoke(DIdStreamState.Error(err))
            DIdStreamState.Error(err)
        }
    }

    suspend fun sendTalk(text: String, voiceId: String) = withContext(Dispatchers.IO) {
        val id = streamId ?: return@withContext
        val sid = sessionId ?: return@withContext
        runCatching {
            val body = JSONObject().apply {
                put("script", JSONObject().apply {
                    put("type", "text")
                    put("input", text)
                    put("provider", JSONObject().apply {
                        put("type", "microsoft")
                        put("voice_id", voiceId)
                    })
                })
                put("session_id", sid)
                put("config", JSONObject().apply {
                    put("fluent", true)
                    put("pad_audio", 0.0)
                })
            }
            postJson("https://api.d-id.com/talks/streams/$id/talks", body.toString())
        }.onFailure { Log.e(TAG, "sendTalk failed: ${it.message}") }
    }

    fun stop() {
        val id = streamId
        streamId = null
        sessionId = null
        if (id != null) {
            ioScope.launch {
                runCatching { deleteRequest("https://api.d-id.com/talks/streams/$id") }
            }
        }
    }

    fun release() {
        stop()
    }

    private fun postJson(url: String, body: String): JSONObject {
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", authHeader)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: "{}"
            if (!resp.isSuccessful) throw RuntimeException("D-ID API ${resp.code}: $text")
            return JSONObject(text)
        }
    }

    private fun deleteRequest(url: String) {
        runCatching {
            httpClient.newCall(
                Request.Builder().url(url)
                    .addHeader("Authorization", authHeader).delete().build(),
            ).execute().close()
        }
    }

    private fun friendlyError(e: Exception): String = when {
        e.message?.contains("401") == true || e.message?.contains("403") == true ->
            "D-ID API key is wrong or expired. Enter your key in Voice & Live Settings."
        e.message?.contains("402") == true || e.message?.contains("insufficient_credits") == true ->
            "Your D-ID account has no credits. Add credits at studio.d-id.com."
        e.message?.contains("Unable to resolve") == true ->
            "No internet connection."
        else -> "D-ID avatar stream failed: ${e.message?.take(120) ?: ""}".trim()
    }

    companion object {
        private const val TAG = "DIdStreaming"

        fun voiceIdFor(gender: String, voiceLabel: String): String {
            val isMale = gender.equals("Male", ignoreCase = true) ||
                voiceLabel.contains("Male", ignoreCase = true)
            return if (isMale) when (voiceLabel) {
                "Deep Male" -> "en-US-GuyNeural"
                "Velvet Male" -> "en-US-GuyNeural"
                "Low Velvet Male" -> "en-US-GuyNeural"
                "Silky Soft Male" -> "en-US-AndrewNeural"
                "Warm Whisper Male" -> "en-US-AndrewNeural"
                "Protective Male" -> "en-US-BrandonNeural"
                "Calm Male" -> "en-US-EricNeural"
                "Motivational Male" -> "en-US-ChristopherNeural"
                "Smooth Male" -> "en-US-GuyNeural"
                "Warm Male" -> "en-US-EricNeural"
                "Soft-Spoken Male" -> "en-US-AndrewNeural"
                else -> "en-US-GuyNeural"
            } else when (voiceLabel) {
                "Soft Female" -> "en-US-SaraNeural"
                "Warm Female" -> "en-US-JennyNeural"
                "Confident Female" -> "en-US-AriaNeural"
                "Sultry Calm Female" -> "en-US-NancyNeural"
                "Bright Female" -> "en-US-AriaNeural"
                "Deep Feminine" -> "en-US-NancyNeural"
                "Gentle Whisper Female" -> "en-US-SaraNeural"
                else -> "en-US-JennyNeural"
            }
        }
    }
}
