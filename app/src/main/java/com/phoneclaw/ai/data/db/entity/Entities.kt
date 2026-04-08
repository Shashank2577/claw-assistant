package com.phoneclaw.ai.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val spaceId: String,
    val title: String,
    val lastMessageAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val thought: String? = null,
    val mediaUri: String? = null,
    val webviewUrl: String? = null,
    val isIframe: Boolean = false,
    val aspectRatio: Float = 1.333f,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "per_chat_settings")
data class PerChatSettingsEntity(
    @PrimaryKey val conversationId: String,
    val modelName: String? = null,
    val systemPrompt: String? = null,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f
)
