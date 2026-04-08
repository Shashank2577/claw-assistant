package com.phoneclaw.ai.data.model

enum class MessageRole(val value: String) {
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool"),
    SYSTEM("system");

    companion object {
        fun fromValue(value: String): MessageRole =
            entries.firstOrNull { it.value == value } ?: USER
    }
}

data class ChatMessageData(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val thought: String? = null,
    val mediaUri: String? = null,
    val webviewUrl: String? = null,
    val isIframe: Boolean = false,
    val aspectRatio: Float = 1.333f,
    val timestamp: Long,
    val isStreaming: Boolean = false,
)

data class ConversationWithLastMessage(
    val id: String,
    val spaceId: String,
    val title: String,
    val lastMessageAt: Long,
    val lastMessagePreview: String? = null,
)
