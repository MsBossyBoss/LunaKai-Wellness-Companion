package com.fancie.aicompanion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ServerConnectionTestStep(
    val name: String,
    val url: String,
    val ok: Boolean,
    val elapsedMs: Long,
    val detail: String,
)

object LunaKaiServerConnectionTester {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    suspend fun run(host: String): List<ServerConnectionTestStep> = withContext(Dispatchers.IO) {
        val cleanHost = LunaKaiLocalConfig.normalizeHost(host)
        listOf(
            test("Ollama", LunaKaiLocalConfig.ollamaBaseUrl(cleanHost)) { body ->
                if (!body.contains("Ollama is running", ignoreCase = true)) throw IOException("Unexpected Ollama root response: ${body.take(120)}")
                "reachable"
            },
            test("Ollama model", LunaKaiLocalConfig.ollamaTagsEndpoint(cleanHost)) { body ->
                val models = JSONObject(body).optJSONArray("models")
                val names = (0 until (models?.length() ?: 0)).mapNotNull { index ->
                    models?.optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }
                }
                if (LunaKaiLocalConfig.OLLAMA_MODEL !in names) throw IOException("model not found; available=${names.joinToString(", ").take(220)}")
                "model found: ${LunaKaiLocalConfig.OLLAMA_MODEL}"
            },
            test("Kokoro", LunaKaiLocalConfig.localKokoroHealthUrl(cleanHost)) { body ->
                val root = JSONObject(body)
                if (!root.optBoolean("ok", false)) throw IOException(root.optString("status").ifBlank { body.take(160) })
                "voice server running; pipeline_loaded=${root.optBoolean("pipeline_loaded", false)}"
            },
            test("faster-whisper", LunaKaiLocalConfig.localSttHealthUrl(cleanHost)) { body ->
                val root = JSONObject(body)
                if (!root.optBoolean("ok", false)) throw IOException(root.optString("status").ifBlank { body.take(160) })
                "STT server running"
            },
            test("Zonos", LunaKaiLocalConfig.localZonosHealthUrl(cleanHost)) { body ->
                val root = JSONObject(body)
                if (!root.optBoolean("ok", false)) throw IOException(root.optString("detail").ifBlank { root.optString("status").ifBlank { body.take(160) } })
                "Zonos server running; status=${root.optString("status", "running")}"
            },
        )
    }

    private fun test(name: String, url: String, validator: (String) -> String): ServerConnectionTestStep {
        val startedAt = System.currentTimeMillis()
        return try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${body.take(240)}")
                ServerConnectionTestStep(name, url, true, System.currentTimeMillis() - startedAt, validator(body))
            }
        } catch (error: Throwable) {
            ServerConnectionTestStep(name, url, false, System.currentTimeMillis() - startedAt, error.message ?: error::class.java.simpleName)
        }
    }
}