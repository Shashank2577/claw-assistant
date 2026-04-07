package com.openclaw.ai.ui.modelpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.ai.data.model.*
import com.openclaw.ai.data.model.ModelDownloadStatusType.*
import com.openclaw.ai.data.repository.DownloadRepository
import com.openclaw.ai.data.repository.ModelRepository
import com.openclaw.ai.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelPickerViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val settingsRepository: SettingsRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val localModels: StateFlow<List<ModelInfo>> = modelRepository.availableModels
        .map { list -> list.filter { it.isLocal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudModels: StateFlow<List<ModelInfo>> = modelRepository.availableModels
        .map { list -> list.filter { it.isCloud } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeModelId: StateFlow<String> = settingsRepository.getDefaultModelId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val downloadStatuses: StateFlow<Map<String, ModelDownloadStatus>> = modelRepository.downloadStatuses
    val downloadProgress: StateFlow<Map<String, Float>> = modelRepository.downloadProgress

    fun selectModel(modelId: String) {
        viewModelScope.launch {
            val model = modelRepository.getModel(modelId) ?: return@launch
            if (model.isLocal) {
                if (modelRepository.isModelDownloaded(modelId)) {
                    settingsRepository.setDefaultModelId(modelId)
                } else {
                    // Trigger download
                    modelRepository.downloadModel(model)
                }
            } else {
                settingsRepository.setDefaultModelId(modelId)
            }
        }
    }
}
