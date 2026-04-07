package com.openclaw.ai.ui.modelpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.ai.data.Model
import com.openclaw.ai.data.ModelDownloadStatus
import com.openclaw.ai.data.repository.ModelRepository
import com.openclaw.ai.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelPickerViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val localModels: StateFlow<List<Model>> = modelRepository.availableModels
        .map { list -> list.filter { it.url.isNotEmpty() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudModels: StateFlow<List<Model>> = modelRepository.availableModels
        .map { list -> list.filter { it.url.isEmpty() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeModelId: StateFlow<String> = settingsRepository.getDefaultModelId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val downloadStatuses: StateFlow<Map<String, ModelDownloadStatus>> = modelRepository.downloadStatuses
    val downloadProgress: StateFlow<Map<String, Float>> = modelRepository.downloadProgress

    fun selectModel(modelName: String) {
        viewModelScope.launch {
            val model = modelRepository.getModel(modelName) ?: return@launch
            if (model.url.isNotEmpty()) {
                if (modelRepository.isModelDownloaded(modelName)) {
                    settingsRepository.setDefaultModelId(modelName)
                } else {
                    modelRepository.downloadModel(model)
                }
            } else {
                settingsRepository.setDefaultModelId(modelName)
            }
        }
    }
}
