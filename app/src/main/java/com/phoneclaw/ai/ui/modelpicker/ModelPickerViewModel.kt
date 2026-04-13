package com.phoneclaw.ai.ui.modelpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneclaw.ai.data.Model
import com.phoneclaw.ai.data.ModelDownloadStatus
import com.phoneclaw.ai.data.repository.ModelRepository
import com.phoneclaw.ai.data.repository.SettingsRepository
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
        .map { models -> models.filter { it.url.isNotEmpty() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudModels: StateFlow<List<Model>> = modelRepository.availableModels
        .map { models -> models.filter { it.url.isEmpty() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeModelId: StateFlow<String?> = modelRepository.activeModel
        .map { it?.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val downloadStatuses: StateFlow<Map<String, ModelDownloadStatus>> = modelRepository.downloadStatuses
    val downloadProgress: StateFlow<Map<String, Float>> = modelRepository.downloadProgress

    fun selectModel(modelName: String) {
        viewModelScope.launch {
            modelRepository.setActiveModel(modelName)
        }
    }

    fun downloadModel(model: Model) {
        viewModelScope.launch {
            modelRepository.downloadModel(model)
        }
    }
}
