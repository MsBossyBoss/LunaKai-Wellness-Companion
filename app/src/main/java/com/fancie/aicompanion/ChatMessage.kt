package com.fancie.aicompanion

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val companionId: String = "",
    val sender: String = "",
    val text: String = "",
    val mode: String = "text",
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isUser: Boolean
        get() = sender == SENDER_USER

    companion object {
        const val SENDER_USER = "user"
        const val SENDER_COMPANION = "companion"
        const val SENDER_SYSTEM = "system"

        const val MODE_TEXT = "text"
        const val MODE_CALL = "call"
        const val MODE_VIDEO = "video"
    }
}

fun stableChatIdForCompanion(companionId: String): String = "chat_$companionId"