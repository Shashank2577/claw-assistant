package com.phoneclaw.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneclaw.ai.data.db.entity.PerChatSettingsEntity
import com.phoneclaw.ai.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PerChatSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PerChatSettingsEntity?>(null)
    val uiState: StateFlow<PerChatSettingsEntity?> = _uiState.asStateFlow()

    fun loadSettings(conversationId: String) {
        viewModelScope.launch {
            settingsRepository.observePerChatSettings(conversationId).collect {
                _uiState.value = it ?: PerChatSettingsEntity(conversationId = conversationId)
            }
        }
    }

    fun updateSettings(settings: PerChatSettingsEntity) {
        viewModelScope.launch {
            settingsRepository.savePerChatSettings(settings)
        }
    }
}
