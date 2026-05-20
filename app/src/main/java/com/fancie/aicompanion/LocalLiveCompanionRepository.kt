package com.fancie.aicompanion

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerGoAway
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveAudioConversationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException

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

data class LocalLiveTurnResult(
    val transcript: String = "",
    val reply: String = "",
    val voiceMessage: String = "",
    val voiceErrorMessage: String? = null,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = errorMessage == null && transcript.isNotBlank() && reply.isNotBlank()
}

class LiveCompanionViewModel : ViewModel() {
    val brainProvider: CompanionBrainProvider = GeminiCompanionBrainProvider()
}

@OptIn(PublicPreviewAPI::class)
class LocalLiveCompanionRepository(
    private val brainProvider: CompanionBrainProvider = GeminiCompanionBrainProvider(),
) {
    companion object {
        private var connected = false
        private var sharedSession: LiveSession? = null
        private var sharedModelName: String = ""

        suspend fun stopSharedAudioConversation() {
            connected = false
            runCatching { sharedSession?.close() }
            sharedSession = null
            sharedModelName = ""
        }

        fun stopSharedAudioPlayback() {
            runCatching { sharedSession?.stopAudioConversation() }
        }
    }

    suspend fun startAudioConversation(
        context: Context,
        companion: CompanionContext,
        modeLabel: String,
        answerPhrase: String? = null,
        onPhase: (LiveCallPhase, String) -> Unit = { _, _ -> },
    ): LocalLiveSessionState = withContext(Dispatchers.IO) {
        val modelName = companion.geminiLiveAudioModel.liveAudioModelOrDefault()
        val setupMessage = companion.geminiLiveSetupMessage(context)
        if (setupMessage != null) {
            connected = false
            Log.i(
                "LunaKaiGeminiLive",
                "liveSessionSetupNeeded liveApi=Gemini liveAudioModel=${modelName.safeLiveLog()} geminiKeyConfigured=${companion.geminiApiKey.isNotBlank()} firebaseAiConfigured=${context.firebaseAiConfigured()} reason=${setupMessage.safeLiveLog()} normalChatProviderUsed=false lunaKaiAiAdultUsed=false androidTtsActive=false androidSpeechActive=false",
            )
            onPhase(LiveCallPhase.Error, setupMessage)
            return@withContext LocalLiveSessionState.Error(setupMessage)
        }

        runCatching {
            onPhase(LiveCallPhase.Calling, "Connecting Gemini Live Voice...")
            stopSharedAudioConversation()
            val session = buildLiveModel(companion, modeLabel, answerPhrase).connect()
            sharedSession = session
            sharedModelName = modelName
            connected = true
            startAudioOnSession(session, context, companion, onPhase)
            Log.i(
                "LunaKaiGeminiLive",
                "liveSessionConnected liveApi=Gemini liveAudioModel=${modelName.safeLiveLog()} responseModality=audio geminiKeyConfigured=true firebaseAiConfigured=true normalChatProviderUsed=false lunaKaiAiAdultUsed=false androidTtsUsed=false androidSpeechUsed=false package=${context.packageName}",
            )
            onPhase(LiveCallPhase.Listening, "Listening")
            LocalLiveSessionState.Connected(
                message = "Gemini Live Voice connected. Speak naturally; ${companion.companionName} will answer with audio.",
                voiceSetupMessage = null,
            )
        }.getOrElse { error ->
            connected = false
            sharedSession = null
            val message = "Gemini Live Voice setup needed. Live chat is available. Live voice audio is not configured yet. ${error.liveErrorDetail()}"
            Log.w(
                "LunaKaiGeminiLive",
                "liveSessionFailed liveApi=Gemini liveAudioModel=${modelName.safeLiveLog()} exception=${error::class.java.simpleName} message=${error.message?.safeLiveLog()} normalChatProviderUsed=false lunaKaiAiAdultUsed=false androidTtsUsed=false androidSpeechUsed=false package=${context.packageName}",
                error,
            )
            onPhase(LiveCallPhase.Error, message)
            LocalLiveSessionState.Error(message, error)
        }
    }

    suspend fun previewCompanionVoice(
        context: Context,
        companion: CompanionContext,
        previewText: String,
        onPhase: (LiveCallPhase, String) -> Unit = { _, _ -> },
    ): LocalLiveSessionState = withContext(Dispatchers.IO) {
        val fixedPreviewText = previewText.trim().ifBlank { "Hey, I'm ${companion.companionName}." }
        val modelName = companion.geminiLiveAudioModel.liveAudioModelOrDefault()
        val setupMessage = companion.geminiLiveSetupMessage(context)
        if (setupMessage != null) {
            Log.i(
                "LunaKaiVoicePreview",
                "previewSetupNeeded liveApi=Gemini liveAudioModel=${modelName.safeLiveLog()} previewText=fixedSample chatMessageCreated=false liveTurnCreated=false companionSelfReply=false lunaKaiAiAdultUsed=false androidTtsUsed=false androidSpeechUsed=false reason=${setupMessage.safeLiveLog()}",
            )
            onPhase(LiveCallPhase.Error, setupMessage)
            return@withContext LocalLiveSessionState.Error(setupMessage)
        }

        var previewSession: LiveSession? = null
        runCatching {
            onPhase(LiveCallPhase.Calling, "Starting Gemini fixed voice preview...")
            previewSession = buildLiveModel(companion, "fixed voice preview", null).connect()
            val session = previewSession ?: throw IOException("Gemini Live preview session was not created.")
            session.send("Speak this exact fixed sample once, with no added words: $fixedPreviewText")
            var playedAudio = false
            try {
                withTimeout(20_000L) {
                    session.receive().collect { message ->
                        if (message is LiveServerContent) {
                            val audioParts = message.content?.parts?.filterIsInstance<InlineDataPart>().orEmpty()
                            if (audioParts.isNotEmpty()) {
                                onPhase(LiveCallPhase.Speaking, "Playing Gemini fixed voice preview.")
                            }
                            audioParts.forEach { part ->
                                playPcm16Mono24khz(part.inlineData)
                                playedAudio = true
                            }
                            if (message.turnComplete) throw PreviewTurnComplete()
                        }
                    }
                }
            } catch (_: PreviewTurnComplete) {
                // Expected path once Gemini finishes the fixed preview turn.
            }
            if (!playedAudio) throw IOException("Gemini Live returned no audio for the fixed preview sample.")
            Log.i(
                "LunaKaiVoicePreview",
                "previewSuccess liveApi=Gemini liveAudioModel=${modelName.safeLiveLog()} previewText=fixedSample chatMessageCreated=false liveTurnCreated=false companionSelfReply=false lunaKaiAiAdultUsed=false androidTtsUsed=false androidSpeechUsed=false",
            )
            LocalLiveSessionState.Connected(
                message = "Gemini fixed voice preview played.",
                voiceSetupMessage = null,
            )
        }.getOrElse { error ->
            val message = "Gemini Live Voice setup needed. Live chat is available. Live voice audio is not configured yet. ${error.liveErrorDetail()}"
            Log.w(
                "LunaKaiVoicePreview",
                "previewFailed liveApi=Gemini liveAudioModel=${modelName.safeLiveLog()} exception=${error::class.java.simpleName} message=${error.message?.safeLiveLog()} chatMessageCreated=false liveTurnCreated=false companionSelfReply=false lunaKaiAiAdultUsed=false androidTtsUsed=false androidSpeechUsed=false",
                error,
            )
            onPhase(LiveCallPhase.Error, message)
            LocalLiveSessionState.Error(message, error)
        }.also {
            runCatching { previewSession?.close() }
        }
    }

    suspend fun captureUserSpeechTurn(
        context: Context,
        companion: CompanionContext,
        history: List<CompanionChatTurn>,
        onPhase: (LiveCallPhase, String) -> Unit = { _, _ -> },
        onTranscript: (String) -> Unit = {},
    ): LocalLiveTurnResult = withContext(Dispatchers.IO) {
        val modelName = companion.geminiLiveAudioModel.liveAudioModelOrDefault()
        val setupMessage = companion.geminiLiveSetupMessage(context)
        if (setupMessage != null) {
            Log.i(
                "LunaKaiGeminiLive",
                "liveVoiceSetupNeeded liveApi=Gemini liveAudioModel=${modelName.safeLiveLog()} geminiKeyConfigured=${companion.geminiApiKey.isNotBlank()} firebaseAiConfigured=${context.firebaseAiConfigured()} normalChatProviderUsed=false lunaKaiAiAdultUsed=false androidTtsUsed=false androidSpeechUsed=false package=${context.packageName} historyTurns=${history.size}",
            )
            onPhase(LiveCallPhase.Error, setupMessage)
            onTranscript("")
            return@withContext LocalLiveTurnResult(voiceErrorMessage = setupMessage)
        }

        val existingSession = sharedSession
        if (connected && existingSession != null) {
            if (!existingSession.isAudioConversationActive()) {
                startAudioOnSession(existingSession, context, companion, onPhase)
            }
            onPhase(LiveCallPhase.Listening, "Listening")
            return@withContext LocalLiveTurnResult(voiceMessage = "Gemini Live Voice is listening. Speak naturally; ${companion.companionName} will answer with audio.")
        }

        when (val state = startAudioConversation(context, companion, "mic", null, onPhase)) {
            is LocalLiveSessionState.Connected -> LocalLiveTurnResult(voiceMessage = state.message)
            is LocalLiveSessionState.Error -> LocalLiveTurnResult(voiceErrorMessage = state.message)
            LocalLiveSessionState.Idle,
            LocalLiveSessionState.Connecting -> LocalLiveTurnResult(voiceMessage = "Connecting Gemini Live Voice...")
        }
    }

    suspend fun submitTextTurn(
        context: Context,
        companion: CompanionContext,
        transcript: String,
        history: List<CompanionChatTurn>,
        onPhase: (LiveCallPhase, String) -> Unit = { _, _ -> },
    ): LocalLiveTurnResult = withContext(Dispatchers.IO) {
        if (!connected) {
            connected = true
            Log.i("LunaKaiGeminiLive", "textTurnAutoConnected liveApi=Gemini androidTtsActive=false androidSpeechActive=false package=${context.packageName}")
        }
        val cleanTranscript = transcript.trim()
        if (cleanTranscript.isBlank()) {
            return@withContext LocalLiveTurnResult(errorMessage = "Type a message for Gemini Live Companion.")
        }
        onPhase(LiveCallPhase.GeneratingReply, "Thinking...")
        val replyState = brainProvider.generateReply(companion, cleanTranscript, history)
        val reply = when (replyState) {
            CompanionBrainState.Loading -> return@withContext LocalLiveTurnResult(transcript = cleanTranscript, errorMessage = "Gemini is still thinking.")
            is CompanionBrainState.Error -> return@withContext LocalLiveTurnResult(transcript = cleanTranscript, errorMessage = replyState.message)
            is CompanionBrainState.Success -> replyState.text.trim()
        }
        Log.i(
            "LunaKaiGeminiLive",
            "liveTextTurnSuccess liveApi=Gemini geminiModel=${companion.geminiModel.safeLiveLog()} replyChars=${reply.length} androidTtsUsed=false androidSpeechUsed=false lunaKaiAiAdultUsed=false",
        )
        LocalLiveTurnResult(transcript = cleanTranscript, reply = reply)
    }

    suspend fun stopAudioConversation() {
        stopSharedAudioConversation()
    }

    fun interruptCompanionSpeech() = stopSharedAudioPlayback()
    fun stopCurrentAudio() = stopSharedAudioPlayback()
    fun requestStopRecording() = stopSharedAudioPlayback()
    fun isConnected(): Boolean = connected && sharedSession != null

    private fun buildLiveModel(
        companion: CompanionContext,
        modeLabel: String,
        answerPhrase: String?,
    ) = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
        modelName = companion.geminiLiveAudioModel.liveAudioModelOrDefault(),
        generationConfig = liveGenerationConfig {
            responseModality = ResponseModality.AUDIO
            speechConfig = SpeechConfig(voice = Voice(companion.geminiLiveVoiceName()))
            inputAudioTranscription = AudioTranscriptionConfig()
            outputAudioTranscription = AudioTranscriptionConfig()
        },
        systemInstruction = content {
            text(companion.liveVoiceSystemInstruction(modeLabel, answerPhrase))
        },
    )

    @SuppressLint("MissingPermission")
    private suspend fun startAudioOnSession(
        session: LiveSession,
        context: Context,
        companion: CompanionContext,
        onPhase: (LiveCallPhase, String) -> Unit,
    ) {
        session.startAudioConversation(
            liveAudioConversationConfig {
                transcriptHandler = { input, output ->
                    val inputText = input?.text.orEmpty().trim()
                    if (inputText.isNotBlank()) {
                        onPhase(LiveCallPhase.ProcessingSpeech, "Heard: ${inputText.take(100)}")
                    }
                    val outputText = output?.text.orEmpty().trim()
                    if (outputText.isNotBlank()) {
                        onPhase(LiveCallPhase.Speaking, "${companion.companionName}: ${outputText.take(160)}")
                    }
                }
                goAwayHandler = { _: LiveServerGoAway ->
                    connected = false
                    onPhase(LiveCallPhase.Ended, "Gemini Live session ended. Start a new call to reconnect.")
                }
                enableInterruptions = false
            },
        )
        Log.i(
            "LunaKaiGeminiLive",
            "liveAudioConversationStarted liveApi=Gemini liveAudioModel=${companion.geminiLiveAudioModel.liveAudioModelOrDefault().safeLiveLog()} voice=${companion.geminiLiveVoiceName().safeLiveLog()} androidAudioRecordUsedByFirebaseLive=true androidTtsUsed=false androidSpeechUsed=false package=${context.packageName}",
        )
    }
}

private const val GEMINI_LIVE_SETUP_BASE =
    "Gemini Live Voice setup needed. Live chat is available. Live voice audio is not configured yet."

private fun CompanionContext.geminiLiveSetupMessage(context: Context): String? {
    if (geminiApiKey.isBlank()) {
        return "$GEMINI_LIVE_SETUP_BASE Missing Gemini API key in Settings > AI Provider Settings."
    }
    if (!context.firebaseAiConfigured()) {
        return "$GEMINI_LIVE_SETUP_BASE Firebase AI Logic is not configured from google-services.json."
    }
    return null
}

private fun Context.firebaseAiConfigured(): Boolean = runCatching {
    val options = FirebaseApp.getInstance().options
    options.apiKey.isNotBlank() && options.projectId?.isNotBlank() == true && options.applicationId.isNotBlank()
}.getOrDefault(false)

private fun CompanionContext.liveVoiceSystemInstruction(modeLabel: String, answerPhrase: String?): String {
    val adultMode = bdsmEnabled && bdsmAdultConsentConfirmed
    val adultGuidance = if (adultMode) {
        "Adult roleplay is enabled by the user. Follow the user's consent and stop/pause words."
    } else {
        "Adult roleplay is not enabled for this live call."
    }
    return """
        You are $companionName in a LunaKai Live Vocal AI $modeLabel.
        Respond as the companion in natural spoken audio.
        Keep replies short, warm, direct, and conversational.
        Do not narrate markdown, labels, or stage directions.
        Do not create duplicate turns or answer yourself.
        $adultGuidance
        ${answerPhrase?.takeIf { it.isNotBlank() }?.let { "Opening line already shown to user: $it" }.orEmpty()}
    """.trimIndent()
}

private fun CompanionContext.geminiLiveVoiceName(): String {
    val normalized = voice.lowercase()
    return when {
        gender.equals("Male", ignoreCase = true) || normalized.contains("male") || normalized.contains("deep") || normalized.contains("low") -> "FENRIR"
        normalized.contains("bright") -> "ZEPHYR"
        normalized.contains("confident") || normalized.contains("deep feminine") -> "KORE"
        normalized.contains("whisper") || normalized.contains("soft") || normalized.contains("warm") -> "UMBriel".uppercase()
        else -> "PUCK"
    }
}

private fun String.liveAudioModelOrDefault(): String =
    trim().ifBlank { LunaKaiLocalConfig.GEMINI_DEFAULT_LIVE_AUDIO_MODEL }

private fun Throwable.liveErrorDetail(): String = when (this) {
    is SecurityException -> "Microphone permission is missing or blocked."
    is IOException -> message?.takeIf { it.isNotBlank() } ?: "Network or audio session setup failed."
    else -> message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
}

private fun String.safeLiveLog(): String = replace(Regex("[\\r\\n]+"), " ").take(180)

private class PreviewTurnComplete : RuntimeException()

private fun playPcm16Mono24khz(audio: ByteArray) {
    if (audio.isEmpty()) return
    val minBufferSize = AudioTrack.getMinBufferSize(
        24_000,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    ).coerceAtLeast(0)
    val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(24_000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build(),
        )
        .setBufferSizeInBytes(maxOf(minBufferSize, audio.size))
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
    try {
        track.play()
        var offset = 0
        while (offset < audio.size) {
            val written = track.write(audio, offset, audio.size - offset)
            if (written <= 0) break
            offset += written
        }
        track.stop()
    } finally {
        track.release()
    }
}