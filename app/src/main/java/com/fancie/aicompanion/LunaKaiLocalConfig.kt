package com.fancie.aicompanion

object LunaKaiLocalConfig {
    const val DEFAULT_SERVER_HOST = "192.168.1.231"
    const val OLLAMA_HOST = DEFAULT_SERVER_HOST
    const val OLLAMA_PORT = 11434
    const val OLLAMA_BASE_URL = "http://$OLLAMA_HOST:$OLLAMA_PORT"
    const val OLLAMA_GENERATE_ENDPOINT = "$OLLAMA_BASE_URL/api/generate"
    const val OLLAMA_TAGS_ENDPOINT = "$OLLAMA_BASE_URL/api/tags"
    const val OLLAMA_MODEL = "lunakai-ai-adult:latest"
    const val GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    const val GEMINI_DEFAULT_MODEL = "gemini-2.5-flash"
    const val GEMINI_DEFAULT_LIVE_AUDIO_MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"

    const val OLLAMA_CONNECT_TIMEOUT_MS = 10_000L
    const val OLLAMA_READ_TIMEOUT_MS = 60_000L
    const val OLLAMA_WRITE_TIMEOUT_MS = 30_000L
    const val OLLAMA_CALL_TIMEOUT_MS = 75_000L
    const val OLLAMA_COLD_START_RETRY_DELAY_MS = 750L
    const val OLLAMA_KEEP_ALIVE = "10m"
    const val OLLAMA_TEMPERATURE = 0.8
    const val OLLAMA_TOP_P = 0.9
    const val OLLAMA_NUM_PREDICT = 100
    const val OLLAMA_GREETING_NUM_PREDICT = 80
    const val OLLAMA_LONG_NUM_PREDICT = 200
    const val OLLAMA_WARMUP_NUM_PREDICT = 1
    const val OLLAMA_NUM_CTX = 1024

    fun normalizeHost(rawHost: String?): String {
        val trimmed = rawHost.orEmpty()
            .trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trim('/')
        val withoutPath = trimmed.substringBefore('/')
        val withoutPort = withoutPath.substringBefore(':')
        return withoutPort.ifBlank { DEFAULT_SERVER_HOST }
    }

    fun ollamaBaseUrl(host: String = DEFAULT_SERVER_HOST): String =
        "http://${normalizeHost(host)}:$OLLAMA_PORT"

    fun ollamaGenerateEndpoint(host: String = DEFAULT_SERVER_HOST): String =
        "${ollamaBaseUrl(host)}/api/generate"

    fun ollamaTagsEndpoint(host: String = DEFAULT_SERVER_HOST): String =
        "${ollamaBaseUrl(host)}/api/tags"
}
