package com.fancie.aicompanion

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val adultRoleplayRepository: AdultRoleplayRepository = AdultRoleplayRepository(),
    private val ollamaRepository: OllamaCompanionRepository = OllamaCompanionRepository(),
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    private val _companionBrainState = MutableStateFlow<CompanionBrainState?>(null)
    val companionBrainState: StateFlow<CompanionBrainState?> = _companionBrainState

    private val _chatStatus = MutableStateFlow<String?>(null)
    val chatStatus: StateFlow<String?> = _chatStatus

    private var listenerRegistration: ListenerRegistration? = null
    private var loadedCompanionId: String? = null
    private var loadedChatId: String? = null
    private val migratedLegacyChatIds = mutableSetOf<String>()
    private var warmupStarted = false

    fun loadMessages(companionId: String) {
        val chatId = stableChatIdForCompanion(companionId)
        if (loadedCompanionId == companionId && loadedChatId == chatId && listenerRegistration != null) return

        listenerRegistration?.remove()
        loadedCompanionId = companionId
        loadedChatId = chatId

        val uid = authProvider().currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "User must be signed in before saving chat.")
            _chatStatus.value = "User must be signed in before saving chat."
            return
        }

        migrateLegacyMessagesIfNeeded(uid, companionId, chatId)

        listenerRegistration = firestoreProvider()
            .collection("users")
            .document(uid)
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Could not load chat messages for $chatId", error)
                    _chatStatus.value = "Chat history could not load yet: ${error.message ?: "check Firestore setup."}"
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _messages.value = snapshot.documents.mapNotNull { document ->
                        val data = document.data ?: return@mapNotNull null
                        ChatMessage(
                            id = document.id,
                            chatId = data["chatId"] as? String ?: chatId,
                            companionId = data["companionId"] as? String ?: companionId,
                            sender = data["sender"] as? String ?: ChatMessage.SENDER_SYSTEM,
                            text = data["text"] as? String ?: "",
                            mode = data["mode"] as? String ?: ChatMessage.MODE_TEXT,
                            createdAt = data["createdAt"].toCreatedAtMillis(),
                        )
                    }
                    _chatStatus.value = null
                }
            }

        warmUpLocalModelIfNeeded()
    }

    private fun migrateLegacyMessagesIfNeeded(uid: String, companionId: String, stableChatId: String) {
        if (!migratedLegacyChatIds.add(stableChatId)) return

        viewModelScope.launch {
            runCatching {
                val firestore = firestoreProvider()
                val stableChatRef = firestore
                    .collection("users")
                    .document(uid)
                    .collection("chats")
                    .document(stableChatId)

                val stableSnapshot = stableChatRef.collection("messages").limit(1).get().awaitTask()
                if (!stableSnapshot.isEmpty) return@runCatching

                val legacyChatIds = listOf(
                    "text_$companionId",
                    "call_$companionId",
                    "video_$companionId",
                ).filterNot { it == stableChatId }

                legacyChatIds.forEach { legacyChatId ->
                    val legacyChatRef = firestore
                        .collection("users")
                        .document(uid)
                        .collection("chats")
                        .document(legacyChatId)

                    val legacyMessages = legacyChatRef.collection("messages")
                        .orderBy("createdAt", Query.Direction.ASCENDING)
                        .get()
                        .awaitTask()

                    if (legacyMessages.isEmpty) return@forEach

                    val batch = firestore.batch()

                    legacyMessages.documents.forEach { document ->
                        val data = document.data.orEmpty().toMutableMap()
                        val messageId = (data["id"] as? String).takeIf { !it.isNullOrBlank() } ?: document.id

                        data["id"] = messageId
                        data["chatId"] = stableChatId
                        data["companionId"] = data["companionId"] as? String ?: companionId
                        data["mode"] = data["mode"] as? String ?: ChatMessage.MODE_TEXT

                        batch.set(stableChatRef.collection("messages").document(messageId), data)
                    }

                    val latest = legacyMessages.documents.maxByOrNull {
                        chatSummaryMillisForViewModel(it.get("createdAt"))
                    }

                    val latestText = latest?.getString("text").orEmpty()

                    batch.set(
                        stableChatRef,
                        mapOf(
                            "companionId" to companionId,
                            "lastMessage" to latestText,
                            "updatedAt" to chatSummaryMillisForViewModel(latest?.get("createdAt")),
                            "mode" to (latest?.getString("mode") ?: ChatMessage.MODE_TEXT),
                        ),
                        SetOptions.merge()
                    )

                    batch.commit().awaitTask()
                }
            }.onFailure { error ->
                Log.w(TAG, "Could not migrate legacy chat messages for $companionId", error)
            }
        }
    }

    private fun warmUpLocalModelIfNeeded(showStatus: Boolean = false) {
        if (warmupStarted) return
        warmupStarted = true
        viewModelScope.launch {
            if (showStatus) _chatStatus.value = "Warming up LunaKai model..."
            val warmed = ollamaRepository.warmUpModel()
            if (showStatus && _chatStatus.value == "Warming up LunaKai model...") {
                _chatStatus.value = if (warmed) null else "Local LunaKai model warm-up did not finish yet. Chat will still try the request."
            }
        }
    }
    fun sendMessage(
        companion: CompanionProfile,
        companionContext: CompanionContext,
        userText: String,
        history: List<CompanionChatTurn>,
        mode: String = ChatMessage.MODE_TEXT,
    ) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isTyping.value) return

        val chatId = stableChatIdForCompanion(companion.id)
        val userMessage = newMessage(chatId, companion.id, ChatMessage.SENDER_USER, trimmed, mode)

        appendOptimistic(userMessage)
        _isTyping.value = true
        _companionBrainState.value = CompanionBrainState.Loading
        _chatStatus.value = "LunaKai is thinking..."
        warmUpLocalModelIfNeeded()

        viewModelScope.launch {
            val waitStatusJob = launch {
                delay(4_500L)
                if (_isTyping.value) _chatStatus.value = "Still waiting on the local model..."
            }

            try {
                launch { saveMessage(userMessage, companion.name) }

                val localContext = companionContext.copy(
                    adultProviderEndpoint = LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT,
                    adultProviderModel = LunaKaiLocalConfig.OLLAMA_MODEL,
                    adultProviderEnabled = true,
                )
                val result = if (
                    localContext.bdsmEnabled &&
                    localContext.bdsmAdultConsentConfirmed &&
                    localContext.adultProviderEnabled
                ) {
                    adultRoleplayRepository.sendMessage(localContext, trimmed, history)
                } else {
                    ollamaRepository.sendMessage(localContext, trimmed, history)
                }

                when (result) {
                    CompanionBrainState.Loading -> Unit

                    is CompanionBrainState.Success -> {
                        val reply = newMessage(
                            chatId = chatId,
                            companionId = companion.id,
                            sender = ChatMessage.SENDER_COMPANION,
                            text = result.text,
                            mode = mode,
                        )
                        appendOptimistic(reply)
                        _companionBrainState.value = result
                        _chatStatus.value = null
                        launch { saveMessage(reply, companion.name) }
                    }

                    is CompanionBrainState.Error -> {
                        val reply = newMessage(
                            chatId = chatId,
                            companionId = companion.id,
                            sender = ChatMessage.SENDER_SYSTEM,
                            text = result.message,
                            mode = mode,
                        )
                        appendOptimistic(reply)
                        _companionBrainState.value = result
                        _chatStatus.value = result.message
                        launch { saveMessage(reply, companion.name) }
                    }
                }
            } finally {
                waitStatusJob.cancel()
                _isTyping.value = false
            }
        }
    }
    fun sendPreparedReply(
        companion: CompanionProfile,
        userText: String,
        companionText: String,
        mode: String = ChatMessage.MODE_TEXT,
    ) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return

        val chatId = stableChatIdForCompanion(companion.id)
        val userMessage = newMessage(chatId, companion.id, ChatMessage.SENDER_USER, trimmed, mode)
        val reply = newMessage(chatId, companion.id, ChatMessage.SENDER_COMPANION, companionText, mode)

        appendOptimistic(userMessage)
        appendOptimistic(reply)

        _companionBrainState.value = CompanionBrainState.Success(companionText)
        _chatStatus.value = null

        viewModelScope.launch {
            saveMessage(userMessage, companion.name)
            saveMessage(reply, companion.name)
        }
    }

    fun addCompanionTranscript(
        companion: CompanionProfile,
        companionText: String,
        mode: String = ChatMessage.MODE_CALL,
    ) {
        val trimmed = companionText.trim()
        if (trimmed.isBlank()) return

        val chatId = stableChatIdForCompanion(companion.id)
        val reply = newMessage(chatId, companion.id, ChatMessage.SENDER_COMPANION, trimmed, mode)
        appendOptimistic(reply)
        _companionBrainState.value = CompanionBrainState.Success(trimmed)
        _chatStatus.value = null

        viewModelScope.launch {
            saveMessage(reply, companion.name)
        }
    }
    private suspend fun saveMessage(message: ChatMessage, companionName: String) {
        val uid = authProvider().currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "User must be signed in before saving chat.")
            _chatStatus.value = "User must be signed in before saving chat."
            return
        }

        runCatching {
            val firestore = firestoreProvider()
            val chatRef = firestore
                .collection("users")
                .document(uid)
                .collection("chats")
                .document(message.chatId)

            val messageRef = chatRef.collection("messages").document(message.id)

            messageRef.set(
                mapOf(
                    "id" to message.id,
                    "chatId" to message.chatId,
                    "companionId" to message.companionId,
                    "sender" to message.sender,
                    "text" to message.text,
                    "mode" to message.mode,
                    "createdAt" to message.createdAt,
                )
            ).awaitTask()

            chatRef.set(
                mapOf(
                    "companionId" to message.companionId,
                    "companionName" to companionName,
                    "lastMessage" to message.text,
                    "updatedAt" to message.createdAt,
                    "mode" to message.mode,
                ),
                SetOptions.merge()
            ).awaitTask()
        }.onFailure { error ->
            Log.w(TAG, "Could not save chat message to ${message.chatId}", error)
            _chatStatus.value = "Message did not save yet: ${error.message ?: "check Firebase Auth/Firestore setup."}"
        }
    }

    private fun appendOptimistic(message: ChatMessage) {
        if (_messages.value.any { it.id == message.id }) return
        _messages.value = (_messages.value + message).sortedBy { it.createdAt }
    }

    private fun newMessage(
        chatId: String,
        companionId: String,
        sender: String,
        text: String,
        mode: String,
    ): ChatMessage = ChatMessage(
        id = UUID.randomUUID().toString(),
        chatId = chatId,
        companionId = companionId,
        sender = sender,
        text = text,
        mode = mode,
        createdAt = System.currentTimeMillis(),
    )

    private fun isLocalLunaKaiAi(endpoint: String): Boolean {
        return endpoint.contains("11434", ignoreCase = true) ||
            endpoint.contains("localhost", ignoreCase = true) ||
            endpoint.contains("192.168.", ignoreCase = true)
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        super.onCleared()
    }

    companion object {
        private const val TAG = "FancieChatViewModel"
    }
}

private fun chatSummaryMillisForViewModel(value: Any?): Long = value.toCreatedAtMillis()

private fun Any?.toCreatedAtMillis(): Long = when (this) {
    is Long -> this
    is Int -> toLong()
    is Double -> toLong()
    is Timestamp -> toDate().time
    else -> System.currentTimeMillis()
}
