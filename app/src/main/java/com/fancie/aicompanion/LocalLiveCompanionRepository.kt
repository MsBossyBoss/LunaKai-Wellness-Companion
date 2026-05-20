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
import com.google.firebase.FirebaseOptions
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerGoAway
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveAudioConversationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.net.URLEncoder
import org.json.JSONObject
import org.json.JSONArray
import okio.ByteString
import okhttp3.WebSocketListener
import okhttp3.WebSocket
import okhttp3.Response
import okhttp3.Request
import okhttp3.OkHttpClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import android.util.Base64
import android.media.MediaRecorder
import android.media.AudioRecord

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
        private var sharedSession: GeminiLiveWebSocketSession? = null
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
            val session = GeminiLiveWebSocketSession(
                context = context.applicationContext,
                companion = companion,
                modeLabel = modeLabel,
                answerPhrase = answerPhrase,
                onPhase = onPhase,
            )
            session.connect()
            sharedSession = session
            sharedModelName = modelName
            connected = true
            session.startAudioConversation()
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

        var previewSession: GeminiLiveWebSocketSession? = null
        runCatching {
            onPhase(LiveCallPhase.Calling, "Starting Gemini fixed voice preview...")
            previewSession = GeminiLiveWebSocketSession(
                context = context.applicationContext,
                companion = companion,
                modeLabel = "fixed voice preview",
                answerPhrase = null,
                onPhase = onPhase,
            )
            val session = previewSession ?: throw IOException("Gemini Live preview session was not created.")
            session.connect()
            session.sendTextTurn("Speak this exact fixed sample once, with no added words: $fixedPreviewText")
            session.awaitFirstAudio(20_000L)
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
                existingSession.startAudioConversation()
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
        context: Context,
        companion: CompanionContext,
        modeLabel: String,
        answerPhrase: String?,
    ) = Firebase.ai(app = context.geminiFirebaseApp(companion), backend = GenerativeBackend.googleAI()).liveModel(
        modelName = companion.geminiLiveAudioModel.liveAudioModelOrDefault(),
        generationConfig = liveGenerationConfig {
            responseModality = ResponseModality.AUDIO
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
            "liveAudioConversationStarted liveApi=Gemini liveAudioModel=${companion.geminiLiveAudioModel.liveAudioModelOrDefault().safeLiveLog()} voice=default androidAudioRecordUsedByFirebaseLive=true androidTtsUsed=false androidSpeechUsed=false package=${context.packageName}",
        )
    }
}

private class GeminiLiveWebSocketSession(
    private val context: Context,
    private val companion: CompanionContext,
    private val modeLabel: String,
    private val answerPhrase: String?,
    private val onPhase: (LiveCallPhase, String) -> Unit,
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val setupComplete = CompletableDeferred<Unit>()
    private val firstAudio = CompletableDeferred<Unit>()
    @Volatile private var active = false
    private var webSocket: WebSocket? = null
    private var recorder: AudioRecord? = null
    private var recordingJob: Job? = null

    suspend fun connect() {
        val apiKey = companion.geminiApiKey.trim()
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=${URLEncoder.encode(apiKey, "UTF-8")}"
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(setupPayload().toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleServerMessage(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                active = false
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                active = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                active = false
                if (!setupComplete.isCompleted) setupComplete.completeExceptionally(t)
                if (!firstAudio.isCompleted) firstAudio.completeExceptionally(t)
                onPhase(LiveCallPhase.Error, "Gemini Live Voice setup needed. Live chat is available. Live voice audio is not configured yet. ${t.liveErrorDetail()}")
            }
        })
        withTimeout(15_000L) { setupComplete.await() }
        active = true
    }

    @SuppressLint("MissingPermission")
    fun startAudioConversation() {
        if (recordingJob?.isActive == true) return
        recordingJob = scope.launch {
            val bufferSize = AudioRecord.getMinBufferSize(
                16_000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(3_200)
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16_000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2,
            )
            recorder = audioRecord
            audioRecord.startRecording()
            onPhase(LiveCallPhase.Listening, "Listening")
            val buffer = ByteArray(3_200)
            while (active && isActive) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) sendAudioChunk(buffer.copyOf(read))
                delay(20L)
            }
        }
    }

    fun isAudioConversationActive(): Boolean = active && recordingJob?.isActive == true

    fun sendTextTurn(text: String) {
        val turn = JSONObject()
            .put("role", "user")
            .put("parts", JSONArray().put(JSONObject().put("text", text)))
        val payload = JSONObject()
            .put("clientContent", JSONObject()
                .put("turns", JSONArray().put(turn))
                .put("turnComplete", true))
        webSocket?.send(payload.toString())
    }

    suspend fun awaitFirstAudio(timeoutMillis: Long) {
        withTimeout(timeoutMillis) { firstAudio.await() }
    }

    fun stopAudioConversation() = close()
    fun stopAudioPlayback() = close()

    fun close() {
        active = false
        recordingJob?.cancel()
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { webSocket?.close(1000, "done") }
        webSocket = null
        scope.cancel()
        client.dispatcher.executorService.shutdown()
    }

    private fun setupPayload(): JSONObject {
        val model = companion.geminiLiveAudioModel.liveAudioModelOrDefault().let { if (it.startsWith("models/")) it else "models/$it" }
        return JSONObject()
            .put("setup", JSONObject()
                .put("model", model)
                .put("generationConfig", JSONObject().put("responseModalities", JSONArray().put("AUDIO"))))
    }

    private fun sendAudioChunk(bytes: ByteArray) {
        val audio = JSONObject()
            .put("mimeType", "audio/pcm;rate=16000")
            .put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
        val payload = JSONObject().put("realtimeInput", JSONObject().put("audio", audio))
        webSocket?.send(payload.toString())
    }

    private fun handleServerMessage(text: String) {
        runCatching {
            val root = JSONObject(text)
            if (root.has("setupComplete")) {
                setupComplete.complete(Unit)
                onPhase(LiveCallPhase.Listening, "Gemini Live Voice connected.")
                return
            }
            val serverContent = root.optJSONObject("serverContent") ?: return
            val outputText = serverContent.optJSONObject("outputTranscription")?.optString("text").orEmpty().trim()
            if (outputText.isNotBlank()) onPhase(LiveCallPhase.Speaking, outputText.take(160))
            val parts = serverContent.optJSONObject("modelTurn")?.optJSONArray("parts")
            var audioPlayed = false
            if (parts != null) {
                for (index in 0 until parts.length()) {
                    val inlineData = parts.optJSONObject(index)?.optJSONObject("inlineData") ?: continue
                    val data = inlineData.optString("data")
                    if (data.isNotBlank()) {
                        val audioBytes = Base64.decode(data, Base64.DEFAULT)
                        playPcm16Mono24khz(audioBytes)
                        audioPlayed = true
                    }
                }
            }
            if (audioPlayed && !firstAudio.isCompleted) firstAudio.complete(Unit)
            if (serverContent.optBoolean("turnComplete", false) && active) onPhase(LiveCallPhase.Listening, "Listening")
        }.onFailure { error ->
            Log.w("LunaKaiGeminiLive", "liveWebSocketMessageParseFailed exception=${error::class.java.simpleName} message=${error.message?.safeLiveLog()} androidTtsUsed=false androidSpeechUsed=false")
        }
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
    options.projectId?.isNotBlank() == true && options.applicationId.isNotBlank()
}.getOrDefault(false)

private val geminiFirebaseAppLock = Any()

private fun Context.geminiFirebaseApp(companion: CompanionContext): FirebaseApp {
    val apiKey = companion.geminiApiKey.trim()
    val defaultApp = FirebaseApp.getInstance()
    val defaultOptions = defaultApp.options
    val appName = "lunakaiGeminiLive${apiKey.firebaseAppNameHash()}"
    synchronized(geminiFirebaseAppLock) {
        FirebaseApp.getApps(applicationContext).firstOrNull { it.name == appName }?.let { return it }
        val builder = FirebaseOptions.Builder()
            .setApplicationId(defaultOptions.applicationId)
            .setApiKey(apiKey)
        defaultOptions.projectId?.takeIf { it.isNotBlank() }?.let(builder::setProjectId)
        defaultOptions.gcmSenderId?.takeIf { it.isNotBlank() }?.let(builder::setGcmSenderId)
        defaultOptions.storageBucket?.takeIf { it.isNotBlank() }?.let(builder::setStorageBucket)
        defaultOptions.databaseUrl?.takeIf { it.isNotBlank() }?.let(builder::setDatabaseUrl)
        return FirebaseApp.initializeApp(applicationContext, builder.build(), appName)
            ?: FirebaseApp.getInstance(appName)
    }
}

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
private fun String.firebaseAppNameHash(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return digest.take(8).joinToString("") { "%02x".format(it) }
}

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
