package com.openclaw.ai.data.repository.impl

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.openclaw.ai.data.*
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
    private val dataStoreRepository: DataStoreRepository,
) : ModelRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _availableModels = MutableStateFlow<List<Model>>(emptyList())
    override val availableModels: StateFlow<List<Model>> = _availableModels.asStateFlow()

    private val _activeModel = MutableStateFlow<Model?>(null)
    override val activeModel: StateFlow<Model?> = _activeModel.asStateFlow()

    private val _downloadStatuses = MutableStateFlow<Map<String, ModelDownloadStatus>>(emptyMap())
    override val downloadStatuses: StateFlow<Map<String, ModelDownloadStatus>> =
        _downloadStatuses.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    override val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val gson = Gson()

    init {
        loadModels()
    }

    private fun loadModels() {
        repositoryScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("model_allowlist.json").bufferedReader().use { it.readText() }
                val allowlist = gson.fromJson(jsonString, ModelAllowlist::class.java)
                val models = allowlist.models.filter { it.disabled != true }.map { it.toModel() }
                
                // Add cloud models bridged for my UI
                val cloudModels = listOf(
                    Model(
                        name = "gemini-2.0-flash",
                        version = "1.0",
                        info = "Fast cloud model.",
                        url = "",
                        isLlm = true
                    ),
                    Model(
                        name = "gemini-2.0-pro",
                        version = "1.0",
                        info = "Capable cloud model.",
                        url = "",
                        isLlm = true
                    )
                )
                
                val allModels = models + cloudModels
                _availableModels.value = allModels
                
                // Initial status sync
                refreshStatuses()
            } catch (e: Exception) {
                Log.e("ModelRepository", "Failed to load models", e)
            }
        }
    }

    private fun refreshStatuses() {
        val statuses = _availableModels.value.associate { model ->
            val isDownloaded = isModelDownloadedSync(model.name)
            model.name to ModelDownloadStatus(
                status = if (isDownloaded) ModelDownloadStatusType.SUCCEEDED else ModelDownloadStatusType.NOT_DOWNLOADED
            )
        }
        _downloadStatuses.value = statuses
    }

    override fun getModel(name: String): Model? =
        _availableModels.value.firstOrNull { it.name == name }

    override fun getLocalModels(): List<Model> =
        _availableModels.value.filter { it.url.isNotEmpty() }

    override fun getCloudModels(): List<Model> =
        _availableModels.value.filter { it.url.isEmpty() }

    override fun getDownloadedModels(): List<Model> =
        _availableModels.value.filter { isModelDownloadedSync(it.name) }

    override suspend fun setActiveModel(modelName: String) {
        _activeModel.value = getModel(modelName)
    }

    override suspend fun downloadModel(
        model: Model,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    ) {
        downloadRepository.downloadModel(
            task = Task(id = "chat", label = "Chat", models = mutableListOf(model), category = Category.LLM),
            model = model,
            onStatusUpdated = { status ->
                _downloadStatuses.update { it + (model.name to status) }
                if (status.totalBytes > 0) {
                    val progress = status.receivedBytes.toFloat() / status.totalBytes
                    _downloadProgress.update { it + (model.name to progress) }
                    onProgress(progress)
                }
                if (status.status == ModelDownloadStatusType.SUCCEEDED) {
                    onComplete()
                } else if (status.status == ModelDownloadStatusType.FAILED) {
                    onError(status.errorMessage)
                }
            }
        )
    }

    override suspend fun cancelDownload(modelName: String) {
        val model = getModel(modelName) ?: return
        downloadRepository.cancelDownload(model)
    }

    override suspend fun deleteDownloadedModel(modelName: String) {
        val model = getModel(modelName) ?: return
        val path = model.getPath(context)
        File(path).deleteRecursively()
        refreshStatuses()
    }

    override suspend fun isModelDownloaded(modelName: String): Boolean = isModelDownloadedSync(modelName)

    private fun isModelDownloadedSync(modelName: String): Boolean {
        val model = getModel(modelName) ?: return false
        if (model.url.isEmpty()) return true // Assume cloud models are "downloaded"
        
        val path = model.getPath(context)
        return File(path).exists()
    }

    override fun observeDownloadProgress(modelName: String): Flow<Float> =
        _downloadProgress.map { it[modelName] ?: 0f }
}
