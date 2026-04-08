package com.phoneclaw.ai.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneclaw.ai.data.Model
import com.phoneclaw.ai.data.ModelDownloadStatusType
import com.phoneclaw.ai.data.repository.ModelRepository
import com.phoneclaw.ai.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val selectedLocalModel: Model? = null,
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
        viewModelScope.launch {
            modelRepository.availableModels.collect { models ->
                if (models.isNotEmpty() && _uiState.value.selectedLocalModel == null) {
                    val gemma = models.find { it.name.contains("Gemma", ignoreCase = true) } ?: models.first()
                    _uiState.update { it.copy(selectedLocalModel = gemma) }
                }
            }
        }

        viewModelScope.launch {
            modelRepository.downloadStatuses.collect { statuses ->
                val model = _uiState.value.selectedLocalModel ?: return@collect
                val status = statuses[model.name]
                if (status != null) {
                    val inProgress = status.status == ModelDownloadStatusType.IN_PROGRESS || 
                                   status.status == ModelDownloadStatusType.UNZIPPING
                    _uiState.update { it.copy(isDownloading = inProgress) }
                    
                    if (status.status == ModelDownloadStatusType.SUCCEEDED && _uiState.value.currentStep == 1) {
                        nextStep()
                    }
                }
            }
        }
        
        viewModelScope.launch {
            modelRepository.downloadProgress.collect { progressMap ->
                val model = _uiState.value.selectedLocalModel ?: return@collect
                val progress = progressMap[model.name] ?: 0f
                _uiState.update { it.copy(downloadProgress = progress) }
            }
        }
    }

    fun nextStep() {
        if (_uiState.value.currentStep < 3) {
            _uiState.update { it.copy(currentStep = it.currentStep + 1) }
        } else {
            completeOnboarding()
        }
    }

    fun prevStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    fun goToStep(step: Int) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun startModelDownload() {
        val model = _uiState.value.selectedLocalModel ?: return
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
        _uiState.update { it.copy(apiKey = key, isApiKeyValid = null) }
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
            _uiState.value.selectedLocalModel?.let { 
                settingsRepository.setDefaultModelId(it.name)
            }
            _uiState.update { it.copy(isComplete = true) }
        }
    }
}
