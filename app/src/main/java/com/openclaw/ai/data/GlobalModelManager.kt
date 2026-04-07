package com.openclaw.ai.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "GlobalModelManager"

/**
 * Singleton class to manage the global model list and their download statuses.
 */
@Singleton
class GlobalModelManager @Inject constructor(
  private val context: Context,
  private val downloadRepository: DownloadRepository,
  private val dataStoreRepository: DataStoreRepository,
) {
  private val coroutineScope = CoroutineScope(Dispatchers.Main)
  private val gson = Gson()

  private val _availableModels = MutableStateFlow<List<Model>>(emptyList())
  val availableModels: StateFlow<List<Model>> = _availableModels.asStateFlow()

  private val _downloadStatuses = MutableStateFlow<Map<String, ModelDownloadStatus>>(emptyMap())
  val downloadStatuses: StateFlow<Map<String, ModelDownloadStatus>> = _downloadStatuses.asStateFlow()

  init {
    loadModels()
  }

  private fun loadModels() {
    coroutineScope.launch(Dispatchers.IO) {
      try {
        val jsonString = context.assets.open("model_allowlist.json").bufferedReader().use { it.readText() }
        val allowlist = gson.fromJson(jsonString, ModelAllowlist::class.java)
        val models = allowlist.models.filter { it.disabled != true }.map { it.toModel() }
        
        // Add cloud models
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
        
        // Sync statuses
        refreshStatuses()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to load models", e)
      }
    }
  }

  private fun refreshStatuses() {
    val statuses = _availableModels.value.associate { model ->
      val isDownloaded = isModelDownloadedSync(model)
      model.name to ModelDownloadStatus(
        status = if (isDownloaded) ModelDownloadStatusType.SUCCEEDED else ModelDownloadStatusType.NOT_DOWNLOADED
      )
    }
    _downloadStatuses.value = statuses
  }

  private fun isModelDownloadedSync(model: Model): Boolean {
    if (model.url.isEmpty()) return true // Assume cloud models are "downloaded"
    val path = model.getPath(context)
    return File(path).exists()
  }

  fun downloadModel(model: Model) {
    downloadRepository.downloadModel(
      task = Task(id = "chat", label = "Chat", models = mutableListOf(model), category = Category.LLM),
      model = model,
      onStatusUpdated = { status ->
        _downloadStatuses.update { it + (model.name to status) }
      }
    )
  }

  fun cancelDownload(model: Model) {
    downloadRepository.cancelDownload(model)
  }

  fun deleteModel(model: Model) {
    val path = model.getPath(context)
    File(path).deleteRecursively()
    refreshStatuses()
  }
}
