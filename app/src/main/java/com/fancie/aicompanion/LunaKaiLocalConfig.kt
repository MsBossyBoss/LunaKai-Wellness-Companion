package com.fancie.aicompanion

object LunaKaiLocalConfig {
    const val OLLAMA_HOST = "192.168.1.231"
    const val OLLAMA_PORT = 11434
    const val OLLAMA_BASE_URL = "http://$OLLAMA_HOST:$OLLAMA_PORT"
    const val OLLAMA_GENERATE_ENDPOINT = "$OLLAMA_BASE_URL/api/generate"
    const val OLLAMA_TAGS_ENDPOINT = "$OLLAMA_BASE_URL/api/tags"
    const val OLLAMA_MODEL = "lunakai-ai-adult:latest"

    const val OLLAMA_CONNECT_TIMEOUT_MS = 15_000L
    const val OLLAMA_READ_TIMEOUT_MS = 90_000L
    const val OLLAMA_WRITE_TIMEOUT_MS = 60_000L
    const val OLLAMA_CALL_TIMEOUT_MS = 90_000L
    const val OLLAMA_COLD_START_RETRY_DELAY_MS = 2_000L
    const val OLLAMA_KEEP_ALIVE = "10m"
    const val OLLAMA_TEMPERATURE = 0.8
    const val OLLAMA_TOP_P = 0.9
    const val OLLAMA_NUM_PREDICT = 180
    const val OLLAMA_WARMUP_NUM_PREDICT = 1
    const val OLLAMA_NUM_CTX = 1024

    const val LOCAL_STT_HEALTH_URL = "http://192.168.1.231:8001/health"
    const val LOCAL_STT_TRANSCRIBE_URL = "http://192.168.1.231:8001/transcribe"
    const val LOCAL_VOICE_HEALTH_URL = "http://192.168.1.231:8000/health"
    const val LOCAL_KOKORO_SPEAK_URL = "http://192.168.1.231:8000/speak/kokoro"
    const val LOCAL_XTTS_SPEAK_URL = "http://192.168.1.231:8000/speak/xtts"
    const val LOCAL_OPENVOICE_SPEAK_URL = "http://192.168.1.231:8000/speak/openvoice"
    const val DEFAULT_LOCAL_VOICE_PROVIDER = "kokoro"
}
