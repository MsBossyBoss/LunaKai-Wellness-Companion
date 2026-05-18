package com.fancie.aicompanion

object LunaKaiLocalConfig {
    const val OLLAMA_BASE_URL = "http://192.168.1.231:11434"
    const val OLLAMA_GENERATE_ENDPOINT = "$OLLAMA_BASE_URL/api/generate"
    const val OLLAMA_MODEL = "lunakai-ai-adult:latest"

    const val LOCAL_STT_TRANSCRIBE_URL = "http://192.168.1.231:8000/transcribe"
    const val LOCAL_KOKORO_SPEAK_URL = "http://192.168.1.231:8000/speak/kokoro"
    const val LOCAL_XTTS_SPEAK_URL = "http://192.168.1.231:8000/speak/xtts"
    const val LOCAL_OPENVOICE_SPEAK_URL = "http://192.168.1.231:8000/speak/openvoice"
}
