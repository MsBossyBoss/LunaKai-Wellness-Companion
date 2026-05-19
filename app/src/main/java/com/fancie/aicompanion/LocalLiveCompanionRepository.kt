package com.fancie.aicompanion

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.sqrt

sealed interface LocalLiveSessionState {
    data object Idle : LocalLiveSessionState
    data object Connecting : LocalLiveSessionState
    data class Connected(val message: String, val voiceSetupMessage: String? = null) : LocalLiveSessionState
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
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = errorMessage == null && transcript.isNotBlank()
}

interface SpeechToTextProvider {
    suspend fun transcribe(audioBytes: ByteArray, mimeType: String = "audio/wav"): LocalSpeechToTextResult
}

class LocalFasterWhisperProvider(
    private val transcribeUrl: String = LunaKaiLocalConfig.LOCAL_STT_TRANSCRIBE_URL,
    private val client: OkHttpClient = localRealtimeHttpClient(),
) : SpeechToTextProvider {
    override suspend fun transcribe(audioBytes: ByteArray, mimeType: String): LocalSpeechToTextResult = withContext(Dispatchers.IO) {
        if (audioBytes.isEmpty()) {
            return@withContext LocalSpeechToTextResult("", errorMessage = "No speech audio was captured. Try again after the local microphone pipeline is connected.")
        }
        val startedAt = System.currentTimeMillis()
        runCatching {
            Log.i("LunaKaiLocalSTT", "transcribeStart url=$transcribeUrl audioBytes=${audioBytes.size} mimeType=$mimeType")
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "lunakai_speech.wav", audioBytes.toRequestBody(mimeType.toMediaType()))
                .build()
            val request = Request.Builder()
                .url(transcribeUrl)
                .post(requestBody)
                .build()
            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${bodyText.take(500)}")
                val root = JSONObject(bodyText)
                val transcript = root.optString("text").trim()
                val durationMs = root.optLong("duration_ms", 0L)
                Log.i("LunaKaiLocalSTT", "transcribeSuccess responseMs=${System.currentTimeMillis() - startedAt} chars=${transcript.length} durationMs=$durationMs")
                LocalSpeechToTextResult(
                    transcript = transcript,
                    partialTranscript = root.optString("partial").trim().takeIf { it.isNotBlank() && it != "false" },
                    durationMs = durationMs,
                )
            }
        }.getOrElse { error ->
            Log.w("LunaKaiLocalSTT", "transcribeFailed responseMs=${System.currentTimeMillis() - startedAt} url=$transcribeUrl exception=${error::class.java.simpleName} message=${error.message}", error)
            LocalSpeechToTextResult(
                transcript = "",
                errorMessage = "Local faster-whisper is not reachable at $transcribeUrl. Start the local STT server before using live speech. Details: ${error.message}",
            )
        }
    }
}

enum class LocalVoiceProviderId(val label: String) {
    Zonos("Zonos"),
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
) {
    val providerVoiceId: String get() = voiceId
}

data class LocalVoiceResult(
    val providerLabel: String,
    val voiceId: String,
    val audioBytes: ByteArray? = null,
    val mimeType: String = "audio/wav",
    val message: String = "",
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = errorMessage == null && audioBytes != null && audioBytes.isNotEmpty()
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
    private val client: OkHttpClient = localRealtimeHttpClient(),
) : VoiceProvider {
    override suspend fun speak(text: String, voiceProfile: CompanionVoiceProfile): LocalVoiceResult = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return@withContext LocalVoiceResult(displayName, voiceProfile.providerVoiceId, errorMessage = "No voice text was provided.")
        }
        val startedAt = System.currentTimeMillis()
        Log.i("LunaKaiLocalVoice", "voiceRequest provider=$displayName speakUrl=$speakUrl voiceId=${voiceProfile.providerVoiceId} voiceLabel=${voiceProfile.label} gender=${voiceProfile.gender} textChars=${trimmed.length}")
        runCatching {
            val payload = JSONObject()
                .put("text", trimmed.take(900))
                .put("voice_id", voiceProfile.providerVoiceId)
                .put("voiceId", voiceProfile.providerVoiceId)
                .put("voiceLabel", voiceProfile.label)
                .put("gender", voiceProfile.gender)
                .put("speed", 1.0)
                .put("format", "wav")
            val request = Request.Builder()
                .url(speakUrl)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body ?: throw IOException("Empty voice response body")
                val contentType = responseBody.contentType()?.toString().orEmpty()
                val bytes = responseBody.bytes()
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: ${String(bytes, Charsets.UTF_8).take(500)}")
                }
                if (contentType.contains("json", ignoreCase = true)) {
                    val root = JSONObject(String(bytes, Charsets.UTF_8))
                    val audioBase64 = root.optString("audio_base64").ifBlank { root.optString("audioBase64") }
                    if (audioBase64.isNotBlank()) {
                        val decoded = android.util.Base64.decode(audioBase64, android.util.Base64.DEFAULT)
                        Log.i("LunaKaiLocalVoice", "voiceSuccessJson provider=$displayName voiceId=${voiceProfile.providerVoiceId} responseMs=${System.currentTimeMillis() - startedAt} audioBytes=${decoded.size}")
                        return@use LocalVoiceResult(displayName, voiceProfile.providerVoiceId, decoded, message = "Voice audio returned by $displayName.")
                    }
                    throw IOException(root.optString("error").ifBlank { root.optString("detail").ifBlank { "Voice server returned JSON without audio bytes." } })
                }
                if (bytes.isEmpty()) throw IOException("Voice server returned empty audio bytes.")
                Log.i("LunaKaiLocalVoice", "voiceSuccess provider=$displayName voiceId=${voiceProfile.providerVoiceId} responseMs=${System.currentTimeMillis() - startedAt} audioBytes=${bytes.size} contentType=$contentType")
                LocalVoiceResult(displayName, voiceProfile.providerVoiceId, bytes, mimeType = contentType.ifBlank { "audio/wav" }, message = "Voice audio returned by $displayName.")
            }
        }.getOrElse { error ->
            Log.w("LunaKaiLocalVoice", "voiceFailed provider=$displayName speakUrl=$speakUrl voiceId=${voiceProfile.providerVoiceId} responseMs=${System.currentTimeMillis() - startedAt} exception=${error::class.java.simpleName} message=${error.message}", error)
            LocalVoiceResult(
                providerLabel = displayName,
                voiceId = voiceProfile.providerVoiceId,
                errorMessage = "$displayName voice server is not reachable or did not return playable audio at $speakUrl. Start the local voice server or choose another local provider. Details: ${error.message}",
            )
        }
    }
}

class LocalZonosVoiceProvider(speakUrl: String = LunaKaiLocalConfig.LOCAL_ZONOS_SPEAK_URL) : LocalHttpVoiceProvider(
    LocalVoiceProviderId.Zonos,
    "Zonos",
    speakUrl,
)

class LocalKokoroVoiceProvider(speakUrl: String = LunaKaiLocalConfig.LOCAL_KOKORO_SPEAK_URL) : LocalHttpVoiceProvider(
    LocalVoiceProviderId.Kokoro,
    "Kokoro",
    speakUrl,
)

class LocalXttsVoiceProvider(speakUrl: String = LunaKaiLocalConfig.LOCAL_XTTS_SPEAK_URL) : LocalHttpVoiceProvider(
    LocalVoiceProviderId.Xtts,
    "XTTS",
    speakUrl,
)

class LocalOpenVoiceProvider(speakUrl: String = LunaKaiLocalConfig.LOCAL_OPENVOICE_SPEAK_URL) : LocalHttpVoiceProvider(
    LocalVoiceProviderId.OpenVoice,
    "OpenVoice",
    speakUrl,
)

object LocalVoiceProviderRegistry {
    fun providerFor(profile: CompanionVoiceProfile, serverHost: String): VoiceProvider =
        providerForId(profile.providerId, serverHost)

    fun providerForId(providerId: LocalVoiceProviderId, serverHost: String): VoiceProvider {
        val host = LunaKaiLocalConfig.normalizeHost(serverHost)
        return when (providerId) {
            LocalVoiceProviderId.Zonos -> LocalZonosVoiceProvider(LunaKaiLocalConfig.localZonosSpeakUrl(host))
            LocalVoiceProviderId.Kokoro -> LocalKokoroVoiceProvider(LunaKaiLocalConfig.localKokoroSpeakUrl(host))
            LocalVoiceProviderId.Xtts -> LocalXttsVoiceProvider(LunaKaiLocalConfig.localXttsSpeakUrl(host))
            LocalVoiceProviderId.OpenVoice -> LocalOpenVoiceProvider(LunaKaiLocalConfig.localOpenVoiceSpeakUrl(host))
        }
    }

    fun providerIdForPreference(raw: String): LocalVoiceProviderId = when (raw.trim().lowercase(Locale.US)) {
        "zonos" -> LocalVoiceProviderId.Zonos
        "xtts" -> LocalVoiceProviderId.Xtts
        "openvoice", "open voice" -> LocalVoiceProviderId.OpenVoice
        else -> LocalVoiceProviderId.Kokoro
    }

    fun profileFor(
        gender: String,
        voiceLabel: String,
        preferredProviderId: LocalVoiceProviderId = LocalVoiceProviderId.Kokoro,
    ): CompanionVoiceProfile {
        val normalizedGender = when {
            gender.equals("Male", ignoreCase = true) || voiceLabel.contains("Male", ignoreCase = true) -> "Male"
            gender.equals("Female", ignoreCase = true) || voiceLabel.contains("Female", ignoreCase = true) || voiceLabel.contains("Feminine", ignoreCase = true) -> "Female"
            else -> "Neutral"
        }
        val voiceId = when (preferredProviderId) {
            LocalVoiceProviderId.Zonos -> zonosVoiceIdFor(normalizedGender)
            LocalVoiceProviderId.Kokoro -> kokoroVoiceIdFor(normalizedGender, voiceLabel)
            LocalVoiceProviderId.Xtts -> "${normalizedGender.lowercase(Locale.US)}_default"
            LocalVoiceProviderId.OpenVoice -> "${normalizedGender.lowercase(Locale.US)}_openvoice"
        }
        return CompanionVoiceProfile(
            label = voiceLabel.ifBlank { if (normalizedGender == "Male") "Deep Smooth Male" else if (normalizedGender == "Female") "Soft Female" else "Neutral Calm" },
            gender = normalizedGender,
            providerId = preferredProviderId,
            voiceId = voiceId,
            previewText = "Hey, I'm your LunaKai companion.",
        )
    }

    fun kokoroFallbackProfile(profile: CompanionVoiceProfile): CompanionVoiceProfile =
        profile.copy(providerId = LocalVoiceProviderId.Kokoro, voiceId = kokoroVoiceIdFor(profile.gender, profile.label))

    private fun kokoroVoiceIdFor(gender: String, voiceLabel: String): String {
        val normalized = voiceLabel.lowercase(Locale.US)
        return when (gender) {
            "Female" -> when {
                "warm" in normalized -> "af_sarah"
                "soft" in normalized || "whisper" in normalized || "bella" in normalized -> "af_bella"
                else -> "af_heart"
            }
            "Male" -> when {
                "deep" in normalized || "low" in normalized || "midnight" in normalized || "velvet" in normalized || "romantic" in normalized -> "am_michael"
                else -> "am_adam"
            }
            else -> "af_heart"
        }
    }

    private fun zonosVoiceIdFor(gender: String): String = when (gender) {
        "Male" -> "male_default"
        "Female" -> "female_default"
        else -> "default"
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
        if (trimmed.equals(lastTranscript, ignoreCase = true) && now - lastTranscriptAtMillis < 1_500L) return false
        lastTranscript = trimmed
        lastTranscriptAtMillis = now
        return true
    }
}

data class LocalLiveTurnResult(
    val transcript: String = "",
    val reply: String = "",
    val voiceMessage: String = "",
    val voiceErrorMessage: String? = null,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = errorMessage == null && transcript.isNotBlank() && reply.isNotBlank()
}

data class CapturedSpeechAudio(
    val wavBytes: ByteArray,
    val durationMs: Long,
    val hadSpeech: Boolean,
)

class LiveCompanionViewModel : ViewModel() {
    val brainProvider: CompanionBrainProvider = OllamaCompanionBrainProvider()
    val sttProvider: SpeechToTextProvider = LocalFasterWhisperProvider()
    val turnTakingController: TurnTakingController = TurnTakingController()
    val avatarPlaybackController: AvatarPlaybackController = AvatarPlaybackController()
}

class LocalLiveCompanionRepository(
    private val brainProvider: CompanionBrainProvider = OllamaCompanionBrainProvider(),
    private val sttProvider: SpeechToTextProvider = LocalFasterWhisperProvider(),
    private val turnTakingController: TurnTakingController = TurnTakingController(),
    private val avatarPlaybackController: AvatarPlaybackController = AvatarPlaybackController(),
    private val voiceWarmupClient: OkHttpClient = localRealtimeHttpClient(),
) {
    companion object {
        private var connected = false
        private var sharedMediaPlayer: MediaPlayer? = null

        suspend fun stopSharedAudioConversation() {
            connected = false
            stopSharedAudioPlayback()
        }

        fun stopSharedAudioPlayback() {
            runCatching { sharedMediaPlayer?.setOnCompletionListener(null) }
            runCatching { sharedMediaPlayer?.setOnErrorListener(null) }
            runCatching { if (sharedMediaPlayer?.isPlaying == true) sharedMediaPlayer?.stop() }
            runCatching { sharedMediaPlayer?.release() }
            sharedMediaPlayer = null
        }
    }

    private suspend fun warmUpKokoroVoice(serverHost: String): String = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val warmupUrl = LunaKaiLocalConfig.localKokoroWarmupUrl(serverHost)
        Log.i("LunaKaiLocalVoice", "voiceWarmupStart url=$warmupUrl")
        runCatching {
            val request = Request.Builder()
                .url(warmupUrl)
                .post(JSONObject().toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            voiceWarmupClient.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                val elapsedMs = System.currentTimeMillis() - startedAt
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${bodyText.take(500)}")
                val pipelineLoaded = runCatching { JSONObject(bodyText).optBoolean("pipeline_loaded", false) }.getOrDefault(false)
                Log.i("LunaKaiLocalVoice", "voiceWarmupSuccess responseMs=$elapsedMs pipelineLoaded=$pipelineLoaded bodyChars=${bodyText.length}")
                ""
            }
        }.getOrElse { error ->
            val elapsedMs = System.currentTimeMillis() - startedAt
            Log.w("LunaKaiLocalVoice", "voiceWarmupFailed responseMs=$elapsedMs url=$warmupUrl exception=${error::class.java.simpleName} message=${error.message}", error)
            "Kokoro voice warmup did not finish at $warmupUrl: ${error.message}"
        }
    }

    suspend fun startAudioConversation(
        context: Context,
        companion: CompanionContext,
        modeLabel: String,
        answerPhrase: String? = null,
        onPhase: (LiveCallPhase, String) -> Unit = { _, _ -> },
    ): LocalLiveSessionState = withContext(Dispatchers.IO) {
        connected = true
        val adultMode = companion.isAdultModeActiveForLive()
        Log.i(
            "LunaKaiModelRoute",
            "liveCompanion provider=Ollama endpoint=${companion.adultProviderEndpoint.ifBlank { LunaKaiLocalConfig.ollamaGenerateEndpoint(companion.serverHost) }} model=${LunaKaiLocalConfig.OLLAMA_MODEL} activeCompanionName=${companion.companionName} activeCompanionMode=${companion.characterMode} adultMode=$adultMode adultPromptIncluded=$adultMode selectedVoice=${companion.voice} localVoiceProvider=${companion.localVoiceProviderId} serverHost=${companion.serverHost} modeLabel=$modeLabel",
        )
        onPhase(LiveCallPhase.GeneratingVoice, "Warming up LunaKai voice...")
        val warmupMessage = withTimeoutOrNull(8_000L) { warmUpKokoroVoice(companion.serverHost) }
            ?: "Kokoro voice warmup is taking longer than expected; continuing with call setup."
        val voiceSetupMessage = warmupMessage.takeIf { it.isNotBlank() }
        val openingLine = answerPhrase?.trim().orEmpty()
        if (openingLine.isBlank()) {
            return@withContext LocalLiveSessionState.Connected("Local live $modeLabel is connected. Tap Mic to record a turn through faster-whisper.", voiceSetupMessage = voiceSetupMessage)
        }
        val voiceResult = speakCompanionText(context, companion, openingLine) { onPhase(LiveCallPhase.Speaking, "Speaking...") }
        if (voiceResult.isSuccess) {
            LocalLiveSessionState.Connected("${voiceResult.providerLabel} played ${voiceResult.voiceId} for the opening answer.")
        } else {
            LocalLiveSessionState.Connected(
                message = "Local live $modeLabel is connected. Transcript works; voice setup still needs attention.",
                voiceSetupMessage = voiceResult.errorMessage ?: voiceSetupMessage ?: "Local voice provider is not ready.",
            )
        }
    }

    suspend fun captureUserSpeechTurn(
        context: Context,
        companion: CompanionContext,
        history: List<CompanionChatTurn>,
        onPhase: (LiveCallPhase, String) -> Unit = { _, _ -> },
    ): LocalLiveTurnResult = withContext(Dispatchers.IO) {
        if (!connected) return@withContext LocalLiveTurnResult(errorMessage = "Live Companion is not connected. Tap Call first.")
        stopCurrentAudio()
        onPhase(LiveCallPhase.Listening, "Listening...")
        Log.i("LunaKaiLocalSTT", "pushToTalkRecordStart connected=$connected")
        val captured = LocalMicrophoneRecorder.recordWav(
            minimumSpeechDurationMillis = turnTakingController.minimumSpeechDurationMillis,
            silenceDetectionMillis = turnTakingController.silenceDetectionMillis,
        )
        if (!captured.hadSpeech) {
            return@withContext LocalLiveTurnResult(errorMessage = "I did not catch enough speech. Try again closer to the microphone.")
        }
        Log.i("LunaKaiLocalSTT", "pushToTalkRecordResult durationMs=${captured.durationMs} hadSpeech=${captured.hadSpeech} wavBytes=${captured.wavBytes.size}")
        onPhase(LiveCallPhase.ProcessingSpeech, "Processing speech...")
        val stt = LocalFasterWhisperProvider(LunaKaiLocalConfig.localSttTranscribeUrl(companion.serverHost)).transcribe(captured.wavBytes)
        if (!stt.isSuccess) {
            return@withContext LocalLiveTurnResult(errorMessage = stt.errorMessage ?: "Local speech transcription did not return text.")
        }
        val transcript = stt.transcript.trim()
        if (!turnTakingController.shouldAcceptFinalTranscript(transcript, captured.durationMs)) {
            return@withContext LocalLiveTurnResult(transcript = transcript, errorMessage = "That sounded too short or duplicated, so LunaKai did not send it again.")
        }
        onPhase(LiveCallPhase.GeneratingReply, "Thinking...")
        val replyState = brainProvider.generateReply(companion, transcript, history)
        val reply = when (replyState) {
            CompanionBrainState.Loading -> return@withContext LocalLiveTurnResult(transcript = transcript, errorMessage = "LunaKai is still thinking.")
            is CompanionBrainState.Error -> return@withContext LocalLiveTurnResult(transcript = transcript, errorMessage = replyState.message)
            is CompanionBrainState.Success -> replyState.text.trim()
        }
        onPhase(LiveCallPhase.GeneratingVoice, "Generating voice...")
        val voiceResult = speakCompanionText(context, companion, reply) { onPhase(LiveCallPhase.Speaking, "Speaking...") }
        LocalLiveTurnResult(
            transcript = transcript,
            reply = reply,
            voiceMessage = voiceResult.message,
            voiceErrorMessage = voiceResult.errorMessage,
        )
    }

    suspend fun speakCompanionText(context: Context, companion: CompanionContext, text: String, onPlaybackStarted: () -> Unit = {}): LocalVoiceResult {
        val selectedProvider = LocalVoiceProviderRegistry.providerIdForPreference(companion.localVoiceProviderId)
        val voiceProfile = LocalVoiceProviderRegistry.profileFor(companion.gender, companion.voice, selectedProvider)
        val provider = LocalVoiceProviderRegistry.providerFor(voiceProfile, companion.serverHost)
        Log.i("LunaKaiLocalVoice", "speakCompanionText selectedVoiceId=${voiceProfile.providerVoiceId} voiceLabel=${voiceProfile.label} provider=${provider.displayName} textChars=${text.trim().length} serverHost=${companion.serverHost}")
        val voiceResult = provider.speak(text, voiceProfile)
        val finalVoiceResult = if (!voiceResult.isSuccess && selectedProvider == LocalVoiceProviderId.Zonos) {
            val fallbackProfile = LocalVoiceProviderRegistry.kokoroFallbackProfile(voiceProfile)
            val fallbackProvider = LocalVoiceProviderRegistry.providerFor(fallbackProfile, companion.serverHost)
            Log.w("LunaKaiLocalVoice", "zonosUnavailableTryingKokoroFallback zonosError=${voiceResult.errorMessage} fallbackVoiceId=${fallbackProfile.providerVoiceId}")
            val fallbackResult = fallbackProvider.speak(text, fallbackProfile)
            if (fallbackResult.isSuccess) fallbackResult.copy(message = "Zonos unavailable; Kokoro fallback played ${fallbackResult.voiceId}.") else voiceResult.copy(errorMessage = "${voiceResult.errorMessage}\nKokoro fallback also failed: ${fallbackResult.errorMessage}")
        } else {
            voiceResult
        }
        if (!finalVoiceResult.isSuccess) return finalVoiceResult
        return playVoiceResult(context, finalVoiceResult, onPlaybackStarted)
    }

    private suspend fun playVoiceResult(context: Context, voiceResult: LocalVoiceResult, onPlaybackStarted: () -> Unit): LocalVoiceResult {
        val audioBytes = voiceResult.audioBytes ?: return voiceResult.copy(errorMessage = "Voice provider returned no audio bytes.")
        val audioFile = withContext(Dispatchers.IO) {
            File(context.cacheDir, "lunakai_voice_${System.currentTimeMillis()}.wav").apply {
                writeBytes(audioBytes)
                Log.i("LunaKaiLocalVoice", "voiceAudioSaved path=$absolutePath audioBytes=${audioBytes.size} mimeType=${voiceResult.mimeType} selectedVoiceId=${voiceResult.voiceId}")
            }
        }
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                stopSharedAudioPlayback()
                avatarPlaybackController.onVoicePlaybackStarted()
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    setDataSource(audioFile.absolutePath)
                    setOnCompletionListener {
                        Log.i("LunaKaiLocalVoice", "playbackComplete path=${audioFile.absolutePath} selectedVoiceId=${voiceResult.voiceId}")
                        avatarPlaybackController.onVoicePlaybackEnded()
                        stopSharedAudioPlayback()
                        runCatching { audioFile.delete() }
                        if (continuation.isActive) continuation.resume(voiceResult.copy(message = "Played ${voiceResult.providerLabel} voice ${voiceResult.voiceId}."))
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.w("LunaKaiLocalVoice", "playbackError path=${audioFile.absolutePath} selectedVoiceId=${voiceResult.voiceId} what=$what extra=$extra")
                        avatarPlaybackController.onVoicePlaybackEnded()
                        stopSharedAudioPlayback()
                        runCatching { audioFile.delete() }
                        if (continuation.isActive) continuation.resume(voiceResult.copy(errorMessage = "Could not play local voice audio. MediaPlayer error what=$what extra=$extra"))
                        true
                    }
                    prepare()
                    Log.i("LunaKaiLocalVoice", "playbackStart path=${audioFile.absolutePath} selectedVoiceId=${voiceResult.voiceId} speed=1.0")
                    onPlaybackStarted()
                    start()
                }
                sharedMediaPlayer = player
                continuation.invokeOnCancellation {
                    avatarPlaybackController.interrupt()
                    stopSharedAudioPlayback()
                    runCatching { audioFile.delete() }
                }
            }
        }
    }

    suspend fun stopAudioConversation() {
        stopSharedAudioConversation()
        avatarPlaybackController.interrupt()
    }

    fun interruptCompanionSpeech() {
        stopSharedAudioPlayback()
        avatarPlaybackController.interrupt()
    }

    fun stopCurrentAudio() {
        interruptCompanionSpeech()
    }

    fun isConnected(): Boolean = connected
}

object LocalMicrophoneRecorder {
    private const val SAMPLE_RATE = 16_000
    private const val SILENCE_RMS_THRESHOLD = 250.0
    private const val MAX_RECORDING_MILLIS = 7_000L

    @SuppressLint("MissingPermission")
    suspend fun recordWav(
        minimumSpeechDurationMillis: Long,
        silenceDetectionMillis: Long,
    ): CapturedSpeechAudio = withContext(Dispatchers.IO) {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(SAMPLE_RATE)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { recorder.release() }
            return@withContext CapturedSpeechAudio(ByteArray(0), 0L, hadSpeech = false)
        }
        val pcm = ByteArrayOutputStream()
        val shortBuffer = ShortArray(minBuffer / 2)
        val startedAt = System.currentTimeMillis()
        var lastVoiceAt = startedAt
        var hadSpeech = false
        Log.i("LunaKaiLocalSTT", "recordStart sampleRate=$SAMPLE_RATE minBuffer=$minBuffer silenceThreshold=$SILENCE_RMS_THRESHOLD minimumSpeechMs=$minimumSpeechDurationMillis silenceMs=$silenceDetectionMillis")
        recorder.startRecording()
        try {
            while (System.currentTimeMillis() - startedAt < MAX_RECORDING_MILLIS) {
                val read = recorder.read(shortBuffer, 0, shortBuffer.size)
                if (read > 0) {
                    var sumSquares = 0.0
                    for (index in 0 until read) {
                        val sample = shortBuffer[index].toInt()
                        pcm.write(sample and 0xFF)
                        pcm.write((sample shr 8) and 0xFF)
                        sumSquares += (sample * sample).toDouble()
                    }
                    val rms = sqrt(sumSquares / read.toDouble())
                    val elapsed = System.currentTimeMillis() - startedAt
                    if (rms > SILENCE_RMS_THRESHOLD) {
                        hadSpeech = true
                        lastVoiceAt = System.currentTimeMillis()
                    }
                    if (hadSpeech && elapsed >= minimumSpeechDurationMillis && System.currentTimeMillis() - lastVoiceAt >= silenceDetectionMillis) {
                        break
                    }
                }
            }
        } finally {
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
        }
        val durationMs = System.currentTimeMillis() - startedAt
        val wavBytes = wavHeader(pcm.size(), SAMPLE_RATE) + pcm.toByteArray()
        Log.i("LunaKaiLocalSTT", "recordStop durationMs=$durationMs hadSpeech=$hadSpeech pcmBytes=${pcm.size()} wavBytes=${wavBytes.size}")
        CapturedSpeechAudio(wavBytes, durationMs, hadSpeech)
    }

    private fun wavHeader(pcmDataSize: Int, sampleRate: Int): ByteArray {
        val totalDataLen = pcmDataSize + 36
        val byteRate = sampleRate * 2
        val header = ByteArrayOutputStream(44)
        header.writeAscii("RIFF")
        header.writeIntLe(totalDataLen)
        header.writeAscii("WAVE")
        header.writeAscii("fmt ")
        header.writeIntLe(16)
        header.writeShortLe(1)
        header.writeShortLe(1)
        header.writeIntLe(sampleRate)
        header.writeIntLe(byteRate)
        header.writeShortLe(2)
        header.writeShortLe(16)
        header.writeAscii("data")
        header.writeIntLe(pcmDataSize)
        return header.toByteArray()
    }

    private fun ByteArrayOutputStream.writeAscii(value: String) = write(value.toByteArray(Charsets.US_ASCII))
    private fun ByteArrayOutputStream.writeIntLe(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }
    private fun ByteArrayOutputStream.writeShortLe(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }
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

private fun localRealtimeHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(90, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .callTimeout(90, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()
