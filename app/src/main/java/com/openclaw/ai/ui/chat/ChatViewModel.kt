package com.openclaw.ai.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.ai.data.Model
import com.openclaw.ai.data.db.entity.MessageEntity
import com.openclaw.ai.data.model.ChatMessageData
import com.openclaw.ai.data.model.MessageRole
import com.openclaw.ai.data.repository.ConversationRepository
import com.openclaw.ai.data.repository.ModelRepository
import com.openclaw.ai.data.repository.SettingsRepository
import com.openclaw.ai.runtime.LlmModelHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val conversationRepository: ConversationRepository,
    private val settingsRepository: SettingsRepository,
    private val modelRepository: ModelRepository,
    private val llmModelHelper: LlmModelHelper,
) : ViewModel() {

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    private val _conversationTitle = MutableStateFlow("New Chat")
    val conversationTitle: StateFlow<String> = _conversationTitle.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageData>>(emptyList())
    val messages: StateFlow<List<ChatMessageData>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    val currentModel: StateFlow<Model?> = modelRepository.activeModel

    private var inferenceJob: Job? = null

    init {
        viewModelScope.launch {
            modelRepository.activeModel.collectLatest { model ->
                if (model != null && model.url.isNotEmpty()) {
                    if (modelRepository.isModelDownloaded(model.name)) {
                        llmModelHelper.initialize(
                            context = context,
                            model = model,
                            supportImage = model.llmSupportImage,
                            supportAudio = model.llmSupportAudio,
                            onDone = { Log.d("ChatViewModel", "Model initialized: $it") },
                            coroutineScope = viewModelScope
                        )
                    }
                }
            }
        }
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            val conversation = conversationRepository.getConversation(conversationId) ?: return@launch
            _currentConversationId.value = conversationId
            _conversationTitle.value = conversation.title

            conversationRepository.getMessages(conversationId).collect { entities ->
                _messages.value = entities.map { it.toChatMessageData() }
            }
        }
    }

    fun sendMessage(text: String, images: List<Bitmap> = emptyList()) {
        val conversationId = _currentConversationId.value ?: return
        val model = modelRepository.activeModel.value ?: return
        if (text.isBlank()) return

        inferenceJob = viewModelScope.launch {
            val userId = UUID.randomUUID().toString()
            conversationRepository.addMessage(MessageEntity(
                id = userId,
                conversationId = conversationId,
                role = MessageRole.USER.value,
                content = text,
                timestamp = System.currentTimeMillis()
            ))

            val assistantId = UUID.randomUUID().toString()
            val placeholder = ChatMessageData(
                id = assistantId,
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = "",
                timestamp = System.currentTimeMillis(),
                isStreaming = true
            )
            _messages.update { it + placeholder }
            _isStreaming.value = true

            var fullResponse = ""
            
            if (model.url.isEmpty()) {
                // Cloud logic placeholder
                _isStreaming.value = false
                _messages.update { list ->
                    list.map { if (it.id == assistantId) it.copy(content = "Cloud support pending", isStreaming = false) else it }
                }
            } else {
                llmModelHelper.runInference(
                    model = model,
                    input = text,
                    images = images,
                    resultListener = { partial, done, _ ->
                        fullResponse += partial
                        _messages.update { list ->
                            list.map { if (it.id == assistantId) it.copy(content = fullResponse, isStreaming = !done) else it }
                        }
                        if (done) {
                            _isStreaming.value = false
                            viewModelScope.launch {
                                conversationRepository.addMessage(MessageEntity(
                                    id = assistantId,
                                    conversationId = conversationId,
                                    role = MessageRole.ASSISTANT.value,
                                    content = fullResponse,
                                    timestamp = System.currentTimeMillis()
                                ))
                            }
                        }
                    },
                    cleanUpListener = {},
                    onError = { error ->
                        _isStreaming.value = false
                        _messages.update { list ->
                            list.map { if (it.id == assistantId) it.copy(content = "Error: $error", isStreaming = false) else it }
                        }
                    },
                    coroutineScope = viewModelScope
                )
            }
        }
    }

    fun stopGeneration() {
        val model = modelRepository.activeModel.value ?: return
        llmModelHelper.stopResponse(model)
        inferenceJob?.cancel()
        _isStreaming.value = false
    }

    override fun onCleared() {
        super.onCleared()
        modelRepository.activeModel.value?.let { llmModelHelper.cleanUp(it) {} }
    }
}

private fun MessageEntity.toChatMessageData(): ChatMessageData = ChatMessageData(
    id = id,
    conversationId = conversationId,
    role = MessageRole.fromValue(role),
    content = content,
    mediaUri = mediaUri,
    timestamp = timestamp
)
