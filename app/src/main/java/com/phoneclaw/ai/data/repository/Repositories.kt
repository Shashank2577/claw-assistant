package com.phoneclaw.ai.data.repository

import com.phoneclaw.ai.data.Model
import com.phoneclaw.ai.data.ModelDownloadStatus
import com.phoneclaw.ai.data.db.entity.ConversationEntity
import com.phoneclaw.ai.data.db.entity.MessageEntity
import com.phoneclaw.ai.data.db.entity.PerChatSettingsEntity
import com.phoneclaw.ai.data.db.entity.SpaceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SpaceRepository {
    fun getAllSpaces(): Flow<List<SpaceEntity>>
    suspend fun createSpace(name: String)
}

interface ConversationRepository {
    fun getConversations(spaceId: String): Flow<List<ConversationEntity>>
    suspend fun getConversation(id: String): ConversationEntity?
    suspend fun createConversation(spaceId: String, title: String): String
    fun getMessages(conversationId: String): Flow<List<MessageEntity>>
    suspend fun addMessage(message: MessageEntity)
    suspend fun deleteAllConversations()
}

interface ModelRepository {
    val availableModels: StateFlow<List<Model>>
    val activeModel: StateFlow<Model?>
    val downloadStatuses: StateFlow<Map<String, ModelDownloadStatus>>
    val downloadProgress: StateFlow<Map<String, Float>>
    
    fun getModel(name: String): Model?
    fun getLocalModels(): List<Model>
    fun getCloudModels(): List<Model>
    fun getDownloadedModels(): List<Model>
    suspend fun setActiveModel(modelName: String)
    suspend fun downloadModel(model: Model, onProgress: (Float) -> Unit = {}, onComplete: () -> Unit = {}, onError: (String) -> Unit = {})
    suspend fun cancelDownload(modelName: String)
    suspend fun deleteDownloadedModel(modelName: String)
    suspend fun isModelDownloaded(modelName: String): Boolean
    fun observeDownloadProgress(modelName: String): Flow<Float>
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }

interface SettingsRepository {
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun getGeminiApiKey(): Flow<String?>
    suspend fun setGeminiApiKey(key: String?)
    suspend fun hasGeminiApiKey(): Boolean
    fun getDefaultModelId(): Flow<String>
    suspend fun setDefaultModelId(modelId: String)
    fun isOnboardingComplete(): Flow<Boolean>
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun getPerChatSettings(conversationId: String): PerChatSettingsEntity?
    fun observePerChatSettings(conversationId: String): Flow<PerChatSettingsEntity?>
    suspend fun savePerChatSettings(settings: PerChatSettingsEntity)
    suspend fun deletePerChatSettings(conversationId: String)
}

interface DownloadRepository {
    fun downloadModel(model: Model)
    fun cancelDownload(modelName: String)
    fun getDownloadStatus(modelId: String): Flow<ModelDownloadStatus>
}
