package com.openclaw.ai.data.repository

import com.openclaw.ai.data.Model
import com.openclaw.ai.data.ModelDownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

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

    suspend fun downloadModel(
        model: Model,
        onProgress: (Float) -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {},
    )

    suspend fun cancelDownload(modelName: String)

    suspend fun deleteDownloadedModel(modelName: String)

    suspend fun isModelDownloaded(modelName: String): Boolean

    fun observeDownloadProgress(modelName: String): Flow<Float>
}
