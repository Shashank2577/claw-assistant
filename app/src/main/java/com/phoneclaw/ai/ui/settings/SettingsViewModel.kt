package com.phoneclaw.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneclaw.ai.data.Model
import com.phoneclaw.ai.data.ModelDownloadStatus
import com.phoneclaw.ai.data.repository.ConversationRepository
import com.phoneclaw.ai.data.repository.ModelRepository
import com.phoneclaw.ai.data.repository.SettingsRepository
import com.phoneclaw.ai.data.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val geminiApiKey: String = "",
    val isApiKeyVisible: Boolean = false,
    val isApiKeyValid: Boolean = true,
    val isTesting: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val modelRepository: ModelRepository,
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = settingsRepository.getThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val availableModels: StateFlow<List<Model>> = modelRepository.availableModels
    val downloadStatuses: StateFlow<Map<String, ModelDownloadStatus>> = modelRepository.downloadStatuses
    val downloadProgress: StateFlow<Map<String, Float>> = modelRepository.downloadProgress

    val defaultModelId: StateFlow<String> = settingsRepository.getDefaultModelId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        viewModelScope.launch {
            settingsRepository.getGeminiApiKey().collect { key ->
                _uiState.update { it.copy(geminiApiKey = key ?: "") }
            }
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setDefaultModel(modelName: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultModelId(modelName)
        }
    }

    fun downloadModel(modelName: String) {
        viewModelScope.launch {
            val model = modelRepository.getModel(modelName) ?: return@launch
            modelRepository.downloadModel(model)
        }
    }

    fun deleteModel(modelName: String) {
        viewModelScope.launch {
            modelRepository.deleteDownloadedModel(modelName)
        }
    }

    fun cancelDownload(modelName: String) {
        viewModelScope.launch {
            modelRepository.cancelDownload(modelName)
        }
    }

    fun onApiKeyChange(key: String) {
        _uiState.update { it.copy(geminiApiKey = key) }
    }

    fun toggleApiKeyVisibility() {
        _uiState.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }
    }

    fun saveApiKey() {
        viewModelScope.launch {
            settingsRepository.setGeminiApiKey(_uiState.value.geminiApiKey.ifBlank { null })
        }
    }

    fun testApiKey() {
        // Implementation
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            conversationRepository.deleteAllConversations()
        }
    }
}
