package com.phoneclaw.ai.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneclaw.ai.data.db.entity.ConversationEntity
import com.phoneclaw.ai.data.db.entity.SpaceEntity
import com.phoneclaw.ai.data.repository.ConversationRepository
import com.phoneclaw.ai.data.repository.SpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val spaceRepository: SpaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrawerUiState())
    val uiState: StateFlow<DrawerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            spaceRepository.getAllSpaces().collect { spaces ->
                _uiState.update { it.copy(spaces = spaces) }
                if (spaces.isNotEmpty() && _uiState.value.currentSpaceId == null) {
                    _uiState.update { it.copy(currentSpaceId = spaces.first().id) }
                }
            }
        }

        viewModelScope.launch {
            _uiState.map { it.currentSpaceId }.distinctUntilChanged().collectLatest { spaceId ->
                if (spaceId != null) {
                    conversationRepository.getConversations(spaceId).collect { conversations ->
                        _uiState.update { it.copy(conversations = conversations) }
                    }
                }
            }
        }
    }

    fun switchSpace(spaceId: String) {
        _uiState.update { it.copy(currentSpaceId = spaceId) }
    }

    fun createNewChat() {
        val spaceId = _uiState.value.currentSpaceId ?: return
        viewModelScope.launch {
            conversationRepository.createConversation(spaceId, "New Chat")
        }
    }

    fun createSpace(name: String) {
        viewModelScope.launch {
            spaceRepository.createSpace(name)
        }
    }

    fun deleteConversation(id: String) {
        // Implementation
    }
}

data class DrawerUiState(
    val spaces: List<SpaceEntity> = emptyList(),
    val currentSpaceId: String? = null,
    val conversations: List<ConversationEntity> = emptyList(),
)
