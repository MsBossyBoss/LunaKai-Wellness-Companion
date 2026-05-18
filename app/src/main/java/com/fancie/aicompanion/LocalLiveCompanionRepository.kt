package com.fancie.aicompanion

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

sealed interface LocalLiveSessionState {
    data object Idle : LocalLiveSessionState
    data object Connecting : LocalLiveSessionState
    data class Connected(val message: String) : LocalLiveSessionState
    data class Error(val message: String, val cause: Throwable? = null) : LocalLiveSessionState
}

enum class LiveCallPhase {
    Idle,
    Calling,
    Ringing,
    Answering,
    Connected,
    Listening,
    ProcessingSpeech,
    GeneratingReply,
    GeneratingVoice,
    Speaking,
    Interrupted,
    Ended,
    Error,
    IncomingCall,
    Accepted,
    Declined,
    Missed,
}

interface CompanionBrainProvider {
    suspend fun generateReply(
        companion: CompanionContext,
        userMessage: String,
        history: List<CompanionChatTurn>,
    ): CompanionBrainState
}

class OllamaCompanionBrainProvider(
    private val repository: OllamaCompanionRepository = OllamaCompanionRepository(),
) : CompanionBrainProvider {
    override suspend fun generateReply(
        companion: CompanionContext,
        userMessage: String,
        history: List<CompanionChatTurn>,
    ): CompanionBrainState = repository.sendMessage(companion, userMessage, history)
}

data class LocalSpeechToTextResult(
    val transcript: String,
    val partialTranscript: String? = null,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = errorMessage == null && transcript.isNotBlank()
}

interface SpeechToTextProvider {
    suspend fun transcribe(audioBytes: ByteArray, mimeType: String = "audio/wav"): LocalSpeechToTextResult
}

class LocalFasterWhisperProvider(
    private val transcribeUrl: String = LunaKaiLocalConfig.LOCAL_STT_TRANSCRIBE_URL,
) : SpeechToTextProvider {
    override suspend fun transcribe(audioBytes: ByteArray, mimeType: String): LocalSpeechToTextResult = withContext(Dispatchers.IO) {
        if (audioBytes.isEmpty()) {
            return@withContext LocalSpeechToTextResult("", errorMessage = "No speech audio was captured. Try again after the local microphone pipeline is connected.")
        }
        runCatching {
            val payload = JSONObject()
                .put("mimeType", mimeType)
                .put("audioBase64", android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP))
            val response = postJson(transcribeUrl, payload, 8_000, 45_000)
            val root = JSONObject(response)
            LocalSpeechToTextResult(
                transcript = root.optString("text").trim(),
                partialTranscript = root.optString("partial").trim().takeIf { it.isNotBlank() },
            )
        }.getOrElse { error ->
            LocalSpeechToTextResult(
                transcript = "",
                errorMessage = "Local faster-whisper is not reachable at $transcribeUrl. Start the local STT server before using live speech. Details: ${error.message}",
            )
        }
    }
}

enum class LocalVoiceProviderId(val label: String) {
    Kokoro("Kokoro"),
    Xtts("XTTS"),
    OpenVoice("OpenVoice"),
}

data class CompanionVoiceProfile(
    val label: String,
    val gender: String,
    val providerId: LocalVoiceProviderId,
    val voiceId: String,
    val previewText: String,
)

data class LocalVoiceResult(
    val providerLabel: String,
    val voiceId: String,
    val audioUrl: String? = null,
    val message: String = "",
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = errorMessage == null
}

interface VoiceProvider {
    val providerId: LocalVoiceProviderId
    val displayName: String
    suspend fun speak(text: String, voiceProfile: CompanionVoiceProfile): LocalVoiceResult
}

abstract class LocalHttpVoiceProvider(
    final override val providerId: LocalVoiceProviderId,
    final override val displayName: String,
    private val speakUrl: String,
) : VoiceProvider {
    override suspend fun speak(text: String, voiceProfile: CompanionVoiceProfile): LocalVoiceResult = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return@withContext LocalVoiceResult(displayName, voiceProfile.voiceId, errorMessage = "No voice text was provided.")
        }
        Log.i("LunaKaiLocalVoice", "voiceRequest provider=$displayName speakUrl=$speakUrl voiceId=${voiceProfile.voiceId} voiceLabel=${voiceProfile.label} gender=${voiceProfile.gender} textChars=${trimmed.length}")
        runCatching {
            val payload = JSONObject()
                .put("text", trimmed.take(500))
                .put("voiceId", voiceProfile.voiceId)
                .put("voiceLabel", voiceProfile.label)
                .put("gender", voiceProfile.gender)
            val response = postJson(speakUrl, payload, 6_000, 60_000)
            val root = JSONObject(response.ifBlank { "{}" })
            Log.i("LunaKaiLocalVoice", "voiceSuccess provider=$displayName voiceId=${voiceProfile.voiceId} responseChars=${response.length}")
            LocalVoiceResult(
                providerLabel = displayName,
                voiceId = voiceProfile.voiceId,
                audioUrl = root.optString("audioUrl").takeIf { it.isNotBlank() },
                message = root.optString("message").ifBlank { "Voice request accepted by $displayName." },
            )
        }.getOrElse { error ->
            Log.w("LunaKaiLocalVoice", "voiceFailed provider=$displayName speakUrl=$speakUrl voiceId=${voiceProfile.voiceId} exception=${error::class.java.simpleName} message=${error.message}", error)
            LocalVoiceResult(
                providerLabel = displayName,
                voiceId = voiceProfile.voiceId,
                errorMessage = "$displayName voice server is not reachable at $speakUrl. Start the local voice server or choose another local provider. Details: ${error.message}",
            )
        }
    }
}

class LocalKokoroVoiceProvider : LocalHttpVoiceProvider(
    LocalVoiceProviderId.Kokoro,
    "Kokoro",
    LunaKaiLocalConfig.LOCAL_KOKORO_SPEAK_URL,
)

class LocalXttsVoiceProvider : LocalHttpVoiceProvider(
    LocalVoiceProviderId.Xtts,
    "XTTS",
    LunaKaiLocalConfig.LOCAL_XTTS_SPEAK_URL,
)

class LocalOpenVoiceProvider : LocalHttpVoiceProvider(
    LocalVoiceProviderId.OpenVoice,
    "OpenVoice",
    LunaKaiLocalConfig.LOCAL_OPENVOICE_SPEAK_URL,
)

object LocalVoiceProviderRegistry {
    private val providers: List<VoiceProvider> = listOf(
        LocalKokoroVoiceProvider(),
        LocalXttsVoiceProvider(),
        LocalOpenVoiceProvider(),
    )

    fun providerFor(profile: CompanionVoiceProfile): VoiceProvider {
        return providers.firstOrNull { it.providerId == profile.providerId } ?: providers.first()
    }

    fun profileFor(gender: String, voiceLabel: String): CompanionVoiceProfile {
        val normalizedGender = when {
            gender.equals("Male", ignoreCase = true) || voiceLabel.contains("Male", ignoreCase = true) -> "Male"
            gender.equals("Female", ignoreCase = true) || voiceLabel.contains("Female", ignoreCase = true) || voiceLabel.contains("Feminine", ignoreCase = true) -> "Female"
            else -> "Neutral"
        }
        val safeId = voiceLabel
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { if (normalizedGender == "Male") "kai_deep_smooth_male" else "luna_soft_female" }
        val provider = when (normalizedGender) {
            "Male" -> LocalVoiceProviderId.Xtts
            "Female" -> LocalVoiceProviderId.Kokoro
            else -> LocalVoiceProviderId.OpenVoice
        }
        val prefix = when (normalizedGender) {
            "Male" -> "kai"
            "Female" -> "luna"
            else -> "lunakai"
        }
        return CompanionVoiceProfile(
            label = voiceLabel.ifBlank { if (normalizedGender == "Male") "Deep Smooth Male" else "Soft Female" },
            gender = normalizedGender,
            providerId = provider,
            voiceId = "${prefix}_$safeId",
            previewText = "Hey, I'm your LunaKai companion.",
        )
    }
}

class AvatarPlaybackController {
    var isSpeaking: Boolean = false
        private set

    fun onVoicePlaybackStarted() {
        isSpeaking = true
    }

    fun onVoicePlaybackEnded() {
        isSpeaking = false
    }

    fun interrupt() {
        isSpeaking = false
    }
}

class TurnTakingController(
    val minimumSpeechDurationMillis: Long = 700L,
    val silenceDetectionMillis: Long = 1_100L,
) {
    private var lastTranscript: String = ""
    private var lastTranscriptAtMillis: Long = 0L

    fun shouldAcceptFinalTranscript(transcript: String, capturedDurationMillis: Long): Boolean {
        val trimmed = transcript.trim()
        if (trimmed.length < 2 || capturedDurationMillis < minimumSpeechDurationMillis) return false
        val now = System.currentTimeMillis()
        if (trimmed == lastTranscript && now - lastTranscriptAtMillis < 1_500L) return false
        lastTranscript = trimmed
        lastTranscriptAtMillis = now
        return true
    }
}

class LiveCompanionViewModel : ViewModel() {
    val brainProvider: CompanionBrainProvider = OllamaCompanionBrainProvider()
    val sttProvider: SpeechToTextProvider = LocalFasterWhisperProvider()
    val turnTakingController: TurnTakingController = TurnTakingController()
    val avatarPlaybackController: AvatarPlaybackController = AvatarPlaybackController()
}

class LocalLiveCompanionRepository {
    companion object {
        private var connected = false

        suspend fun stopSharedAudioConversation() {
            connected = false
        }
    }

    suspend fun startAudioConversation(
        companion: CompanionContext,
        modeLabel: String,
        answerPhrase: String? = null,
    ): LocalLiveSessionState = withContext(Dispatchers.IO) {
        connected = true
        val adultMode = companion.isAdultModeActiveForLive()
        Log.i(
            "LunaKaiModelRoute",
            "liveCompanion provider=Ollama endpoint=${LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT} model=${LunaKaiLocalConfig.OLLAMA_MODEL} activeCompanionName=${companion.companionName} activeCompanionMode=${companion.characterMode} adultMode=$adultMode adultPromptIncluded=$adultMode selectedVoice=${companion.voice} modeLabel=$modeLabel",
        )
        val openingLine = answerPhrase?.trim().orEmpty()
        if (openingLine.isBlank()) {
            return@withContext LocalLiveSessionState.Connected("Local live $modeLabel is ready. Speech-to-text and voice playback require the local faster-whisper and voice servers.")
        }
        val voiceProfile = LocalVoiceProviderRegistry.profileFor(companion.gender, companion.voice)
        val provider = LocalVoiceProviderRegistry.providerFor(voiceProfile)
        val voiceResult = provider.speak(openingLine, voiceProfile)
        if (voiceResult.isSuccess) {
            LocalLiveSessionState.Connected("${provider.displayName} accepted ${voiceProfile.label} (${voiceProfile.voiceId}) for the opening answer.")
        } else {
            Log.w("LunaKaiLocalVoice", voiceResult.errorMessage.orEmpty())
            LocalLiveSessionState.Error(voiceResult.errorMessage ?: "Local voice provider is not ready.")
        }
    }

    suspend fun stopAudioConversation() {
        stopSharedAudioConversation()
    }

    fun isConnected(): Boolean = connected
}


private fun CompanionContext.isAdultModeActiveForLive(): Boolean {
    val roleplayText = (roleplayStyles.joinToString(" ") + " " + characterMode + " " + adultPhrasePreferences)
        .lowercase(Locale.US)
    return adultProviderEnabled && (
        bdsmEnabled ||
            bdsmAdultConsentConfirmed ||
            anatomicalLanguageAllowed ||
            roleplayText.contains("adult") ||
            roleplayText.contains("bdsm") ||
            roleplayText.contains("roleplay") ||
            roleplayText.contains("romantic") ||
            roleplayText.contains("monologue") ||
            roleplayText.contains("acting")
        )
}
private fun postJson(url: String, payload: JSONObject, connectTimeoutMs: Int, readTimeoutMs: Int): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = connectTimeoutMs
        readTimeout = readTimeoutMs
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
    }
    return try {
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
        }
        if (connection.responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IllegalStateException("HTTP ${connection.responseCode}: $errorText")
        }
    } finally {
        connection.disconnect()
    }
}
