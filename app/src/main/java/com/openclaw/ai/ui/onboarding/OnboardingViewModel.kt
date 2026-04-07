package com.openclaw.ai.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.ai.data.model.*
import com.openclaw.ai.data.repository.ModelRepository
import com.openclaw.ai.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val selectedLocalModel: ModelInfo = DefaultModels.GEMMA_3N_2B,
    val downloadProgress: Float = 0f,
    val isDownloading: Boolean = false,
    val apiKey: String = "",
    val isApiKeyValid: Boolean? = null,
    val isComplete: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        // Observe download progress for the onboarding model
        viewModelScope.launch {
            modelRepository.downloadStatuses.collect { statuses ->
                val status = statuses[_uiState.value.selectedLocalModel.id]
                if (status != null) {
                    val inProgress = status.status == ModelDownloadStatusType.IN_PROGRESS || 
                                   status.status == ModelDownloadStatusType.UNZIPPING
                    _uiState.value = _uiState.value.copy(isDownloading = inProgress)
                    
                    if (status.status == ModelDownloadStatusType.SUCCEEDED && _uiState.value.currentStep == 1) {
                        nextStep()
                    }
                }
            }
        }
        
        viewModelScope.launch {
            modelRepository.downloadProgress.collect { progressMap ->
                val progress = progressMap[_uiState.value.selectedLocalModel.id] ?: 0f
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
            }
        }
    }

    fun nextStep() {
        if (_uiState.value.currentStep < 3) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep + 1)
        } else {
            completeOnboarding()
        }
    }

    fun prevStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1)
        }
    }

    fun goToStep(step: Int) {
        _uiState.value = _uiState.value.copy(currentStep = step)
    }

    fun startModelDownload() {
        val model = _uiState.value.selectedLocalModel
        viewModelScope.launch {
            modelRepository.downloadModel(model)
        }
    }

    fun skipModelDownload() {
        nextStep()
    }

    fun skipCloudSetup() {
        completeOnboarding()
    }

    fun onApiKeyChange(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key, isApiKeyValid = null)
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.setGeminiApiKey(key.ifBlank { null })
            completeOnboarding()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingComplete(true)
            // Set the downloaded or selected model as default
            settingsRepository.setDefaultModelId(_uiState.value.selectedLocalModel.id)
            _uiState.value = _uiState.value.copy(isComplete = true)
        }
    }
}
