package com.openclaw.ai.data.repository.impl

import android.content.Context
import android.util.Log
import com.openclaw.ai.data.model.*
import com.openclaw.ai.data.repository.DownloadRepository
import com.openclaw.ai.data.repository.ModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadRepository: DownloadRepository,
) : ModelRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _availableModels = MutableStateFlow(DefaultModels.ALL)
    override val availableModels: StateFlow<List<ModelInfo>> = _availableModels.asStateFlow()

    private val _activeModel = MutableStateFlow<ModelInfo?>(null)
    override val activeModel: StateFlow<ModelInfo?> = _activeModel.asStateFlow()

    private val _downloadStatuses = MutableStateFlow<Map<String, ModelDownloadStatus>>(emptyMap())
    override val downloadStatuses: StateFlow<Map<String, ModelDownloadStatus>> =
        _downloadStatuses.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    override val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    init {
        refreshStatuses()
        // Start observing workers for all local models to catch resumed/running downloads
        getLocalModels().forEach { model ->
            repositoryScope.launch {
                downloadRepository.getDownloadStatus(model.id).collect { status ->
                    _downloadStatuses.value = _downloadStatuses.value + (model.id to status)
                    if (status.status == ModelDownloadStatusType.IN_PROGRESS && status.totalBytes > 0) {
                        val progress = status.receivedBytes.toFloat() / status.totalBytes.toFloat()
                        _downloadProgress.value = _downloadProgress.value + (model.id to progress)
                    } else if (status.status == ModelDownloadStatusType.SUCCEEDED) {
                        _downloadProgress.value = _downloadProgress.value + (model.id to 1f)
                    }
                }
            }
        }
    }

    private fun refreshStatuses() {
        val initialStatuses = _availableModels.value.associate { model ->
            val status = if (model.isCloud) {
                ModelDownloadStatus(ModelDownloadStatusType.SUCCEEDED)
            } else if (isDownloadedSync(model)) {
                ModelDownloadStatus(ModelDownloadStatusType.SUCCEEDED)
            } else {
                ModelDownloadStatus(ModelDownloadStatusType.NOT_DOWNLOADED)
            }
            model.id to status
        }
        _downloadStatuses.value = initialStatuses
    }

    override fun getModel(id: String): ModelInfo? =
        _availableModels.value.firstOrNull { it.id == id }

    override fun getLocalModels(): List<ModelInfo> =
        _availableModels.value.filter { it.isLocal }

    override fun getCloudModels(): List<ModelInfo> =
        _availableModels.value.filter { it.isCloud }

    override fun getDownloadedModels(): List<ModelInfo> =
        _availableModels.value.filter { model ->
            isDownloadedSync(model)
        }

    override suspend fun setActiveModel(modelId: String) {
        _activeModel.value = getModel(modelId)
    }

    override suspend fun downloadModel(
        model: ModelInfo,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    ) {
        downloadRepository.downloadModel(model)
        // Progress is handled by the init-time observer
    }

    override suspend fun cancelDownload(modelId: String) {
        downloadRepository.cancelDownload(modelId)
    }

    override suspend fun deleteDownloadedModel(modelId: String) {
        val model = getModel(modelId) ?: return
        val path = model.getPath(context)
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
        // Also delete version directory if it's empty
        file.parentFile?.let { versionDir ->
            if (versionDir.exists() && versionDir.listFiles()?.isEmpty() == true) {
                versionDir.delete()
                versionDir.parentFile?.let { modelDir ->
                    if (modelDir.exists() && modelDir.listFiles()?.isEmpty() == true) {
                        modelDir.delete()
                    }
                }
            }
        }
        refreshStatuses()
    }

    override suspend fun isModelDownloaded(modelId: String): Boolean {
        val model = getModel(modelId) ?: return false
        return isDownloadedSync(model)
    }

    override fun observeDownloadProgress(modelId: String): Flow<Float> =
        _downloadProgress.map { it[modelId] ?: 0f }

    private fun isDownloadedSync(model: ModelInfo): Boolean {
        if (model.isCloud) return true
        val path = model.getPath(context)
        return File(path).exists()
    }
}
