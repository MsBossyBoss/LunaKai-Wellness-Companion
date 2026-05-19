package com.fancie.aicompanion

object LunaKaiLocalConfig {
    const val DEFAULT_SERVER_HOST = "192.168.1.231"
    const val OLLAMA_HOST = DEFAULT_SERVER_HOST
    const val OLLAMA_PORT = 11434
    const val LOCAL_KOKORO_PORT = 8000
    const val LOCAL_STT_PORT = 8001
    const val LOCAL_ZONOS_PORT = 8002
    const val OLLAMA_BASE_URL = "http://$OLLAMA_HOST:$OLLAMA_PORT"
    const val OLLAMA_GENERATE_ENDPOINT = "$OLLAMA_BASE_URL/api/generate"
    const val OLLAMA_TAGS_ENDPOINT = "$OLLAMA_BASE_URL/api/tags"
    const val OLLAMA_MODEL = "lunakai-ai-adult:latest"

    const val OLLAMA_CONNECT_TIMEOUT_MS = 10_000L
    const val OLLAMA_READ_TIMEOUT_MS = 60_000L
    const val OLLAMA_WRITE_TIMEOUT_MS = 30_000L
    const val OLLAMA_CALL_TIMEOUT_MS = 75_000L
    const val OLLAMA_COLD_START_RETRY_DELAY_MS = 750L
    const val OLLAMA_KEEP_ALIVE = "10m"
    const val OLLAMA_TEMPERATURE = 0.8
    const val OLLAMA_TOP_P = 0.9
    const val OLLAMA_NUM_PREDICT = 160
    const val OLLAMA_WARMUP_NUM_PREDICT = 1
    const val OLLAMA_NUM_CTX = 1024

    val LOCAL_STT_HEALTH_URL = localSttHealthUrl()
    val LOCAL_STT_TRANSCRIBE_URL = localSttTranscribeUrl()
    val LOCAL_VOICE_HEALTH_URL = localKokoroHealthUrl()
    val LOCAL_VOICE_WARMUP_URL = localKokoroWarmupUrl()
    val LOCAL_KOKORO_SPEAK_URL = localKokoroSpeakUrl()
    val LOCAL_XTTS_SPEAK_URL = localXttsSpeakUrl()
    val LOCAL_OPENVOICE_SPEAK_URL = localOpenVoiceSpeakUrl()
    val LOCAL_ZONOS_HEALTH_URL = localZonosHealthUrl()
    val LOCAL_ZONOS_SPEAK_URL = localZonosSpeakUrl()
    const val DEFAULT_LOCAL_VOICE_PROVIDER = "kokoro"

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

    fun localSttHealthUrl(host: String = DEFAULT_SERVER_HOST): String =
        "http://${normalizeHost(host)}:$LOCAL_STT_PORT/health"

    fun localSttTranscribeUrl(host: String = DEFAULT_SERVER_HOST): String =
        "http://${normalizeHost(host)}:$LOCAL_STT_PORT/transcribe"

    fun localKokoroHealthUrl(host: String = DEFAULT_SERVER_HOST): String =
        "http://${normalizeHost(host)}:$LOCAL_KOKORO_PORT/health"

    fun localKokoroWarmupUrl(host: String = DEFAULT_SERVER_HOST): String =
        "http://${normalizeHost(host)}:$LOCAL_KOKORO_PORT/warmup"

    fun localKokoroSpeakUrl(host: String = DEFAULT_SERVER_HOST): String =
        "http://${normalizeHost(host)}:$LOCAL_KOKORO_PORT/speak/kokoro"

    fun localXttsSpeakUrl(host: String = DEFAULT_SERVER_HOST): String =
        "http://${normalizeHost(host)}:$LOCAL_KOKORO_PORT/speak/xtts"

    fun localOpenVoiceSpeakUrl(host: String = DEFAULT_SERVER_HOST): String =
        "http://${normalizeHost(host)}:$LOCAL_KOKORO_PORT/speak/openvoice"

    fun localZonosHealthUrl(host: String = DEFAULT_SERVER_HOST): String =
        "http://${normalizeHost(host)}:$LOCAL_ZONOS_PORT/health"

    fun localZonosSpeakUrl(host: String = DEFAULT_SERVER_HOST): String =
        "http://${normalizeHost(host)}:$LOCAL_ZONOS_PORT/speak/zonos"
}
