package com.phoneclaw.ai.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneclaw.ai.customtasks.agentchat.AgentTools
import com.phoneclaw.ai.customtasks.agentchat.SkillManagerViewModel
import com.phoneclaw.ai.data.DataStoreRepository
import com.phoneclaw.ai.data.Model
import com.phoneclaw.ai.data.db.entity.MessageEntity
import com.phoneclaw.ai.data.model.ChatMessageData
import com.phoneclaw.ai.data.model.MessageRole
import com.phoneclaw.ai.data.repository.ConversationRepository
import com.phoneclaw.ai.data.repository.ModelRepository
import com.phoneclaw.ai.runtime.LlmModelHelper
import com.phoneclaw.ai.runtime.runInference
import com.google.ai.edge.litertlm.tool
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
    private val modelRepository: ModelRepository,
    private val llmModelHelper: LlmModelHelper,
    val agentTools: AgentTools,
    val skillManagerViewModel: SkillManagerViewModel,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessageData>>(emptyList())
    val messages: StateFlow<List<ChatMessageData>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    val currentModel: StateFlow<Model?> = modelRepository.activeModel

    private val _currentConversationId = MutableStateFlow<String?>(null)

    val conversationTitle: StateFlow<String> = _currentConversationId
        .map { "Chat" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Chat")

    private var inferenceJob: Job? = null

    private var _modelInitialized = false
    private var _initializedModelId: String? = null

    init {
        agentTools.context = context
        agentTools.skillManagerViewModel = skillManagerViewModel

        viewModelScope.launch {
            skillManagerViewModel.loadSkills {}
        }
    }

    private suspend fun startNewConversation() {
        val id = conversationRepository.createConversation("default", "New Chat")
        _currentConversationId.value = id
    }

    fun sendMessage(text: String) {
        val model = currentModel.value ?: return
        if (text.isBlank()) return

        inferenceJob = viewModelScope.launch {
            if (_currentConversationId.value == null) {
                startNewConversation()
            }
            val conversationId = _currentConversationId.value ?: "default"

            val userId = UUID.randomUUID().toString()
            val userTimestamp = System.currentTimeMillis()
            val userMsg = ChatMessageData(
                id = userId,
                conversationId = conversationId,
                role = MessageRole.USER,
                content = text,
                timestamp = userTimestamp
            )
            _messages.update { it + userMsg }
            conversationRepository.addMessage(
                MessageEntity(
                    id = userId,
                    conversationId = conversationId,
                    role = MessageRole.USER.value,
                    content = text,
                    timestamp = userTimestamp
                )
            )

            val assistantId = UUID.randomUUID().toString()
            val assistantTimestamp = System.currentTimeMillis()
            val placeholder = ChatMessageData(
                id = assistantId,
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = "",
                timestamp = assistantTimestamp,
                isStreaming = true
            )
            _messages.update { it + placeholder }
            _isStreaming.value = true

            var fullResponse = ""
            var fullThought = ""

            val selectedSkills = skillManagerViewModel.getSelectedSkills()

            val needsInit = !_modelInitialized || _initializedModelId != model.name
            if (needsInit) {
                llmModelHelper.initialize(
                    context = context,
                    model = model,
                    supportImage = model.llmSupportImage,
                    supportAudio = model.llmSupportAudio,
                    onDone = { error ->
                        if (error.isNotEmpty()) {
                            Log.e("ChatViewModel", "Init error: $error")
                            _isStreaming.value = false
                            _messages.update { list ->
                                list.map {
                                    if (it.id == assistantId) it.copy(
                                        content = "Could not load model.\n\nMake sure the model is fully downloaded in the model picker.",
                                        isStreaming = false
                                    ) else it
                                }
                            }
                            return@initialize
                        }
                        _modelInitialized = true
                        _initializedModelId = model.name

                        llmModelHelper.runInference(
                            model = model,
                            input = text,
                            resultListener = { partial, done, thought ->
                                fullResponse += partial
                                if (thought != null) fullThought += thought

                                if (done) {
                                    _isStreaming.value = false
                                    val webview = agentTools.resultWebviewToShow
                                    agentTools.resultWebviewToShow = null

                                    val finalMessage = placeholder.copy(
                                        content = fullResponse,
                                        thought = fullThought.ifEmpty { null },
                                        webviewUrl = webview?.url,
                                        isIframe = webview?.iframe == true,
                                        aspectRatio = webview?.aspectRatio ?: 1.333f,
                                        isStreaming = false
                                    )
                                    _messages.update { list ->
                                        list.map { if (it.id == assistantId) finalMessage else it }
                                    }
                                    viewModelScope.launch {
                                        conversationRepository.addMessage(
                                            MessageEntity(
                                                id = assistantId,
                                                conversationId = conversationId,
                                                role = MessageRole.ASSISTANT.value,
                                                content = fullResponse,
                                                thought = fullThought.ifEmpty { null },
                                                webviewUrl = finalMessage.webviewUrl,
                                                isIframe = finalMessage.isIframe,
                                                aspectRatio = finalMessage.aspectRatio,
                                                timestamp = assistantTimestamp
                                            )
                                        )
                                    }
                                } else {
                                    _messages.update { list ->
                                        list.map {
                                            if (it.id == assistantId) {
                                                it.copy(content = fullResponse, thought = fullThought.ifEmpty { null })
                                            } else it
                                        }
                                    }
                                }
                            },
                            cleanUpListener = {},
                            onError = { err ->
                                _isStreaming.value = false
                                _messages.update { list ->
                                    list.map { if (it.id == assistantId) it.copy(content = "Error: $err", isStreaming = false) else it }
                                }
                            },
                            coroutineScope = viewModelScope
                        )
                    },
                    systemInstruction = if (selectedSkills.isEmpty()) null else skillManagerViewModel.getSystemPrompt("You are a helpful AI assistant with access to tools."),
                    tools = if (selectedSkills.isEmpty()) emptyList() else listOf(tool(agentTools)),
                    enableConversationConstrainedDecoding = selectedSkills.isNotEmpty(),
                    coroutineScope = viewModelScope
                )
            } else {
                llmModelHelper.runInference(
                    model = model,
                    input = text,
                    resultListener = { partial, done, thought ->
                        fullResponse += partial
                        if (thought != null) fullThought += thought

                        if (done) {
                            _isStreaming.value = false
                            val webview = agentTools.resultWebviewToShow
                            agentTools.resultWebviewToShow = null

                            val finalMessage = placeholder.copy(
                                content = fullResponse,
                                thought = fullThought.ifEmpty { null },
                                webviewUrl = webview?.url,
                                isIframe = webview?.iframe == true,
                                aspectRatio = webview?.aspectRatio ?: 1.333f,
                                isStreaming = false
                            )
                            _messages.update { list ->
                                list.map { if (it.id == assistantId) finalMessage else it }
                            }
                            viewModelScope.launch {
                                conversationRepository.addMessage(
                                    MessageEntity(
                                        id = assistantId,
                                        conversationId = conversationId,
                                        role = MessageRole.ASSISTANT.value,
                                        content = fullResponse,
                                        thought = fullThought.ifEmpty { null },
                                        webviewUrl = finalMessage.webviewUrl,
                                        isIframe = finalMessage.isIframe,
                                        aspectRatio = finalMessage.aspectRatio,
                                        timestamp = assistantTimestamp
                                    )
                                )
                            }
                        } else {
                            _messages.update { list ->
                                list.map {
                                    if (it.id == assistantId) {
                                        it.copy(content = fullResponse, thought = fullThought.ifEmpty { null })
                                    } else it
                                }
                            }
                        }
                    },
                    cleanUpListener = {},
                    onError = { err ->
                        _isStreaming.value = false
                        _messages.update { list ->
                            list.map { if (it.id == assistantId) it.copy(content = "Error: $err", isStreaming = false) else it }
                        }
                    },
                    coroutineScope = viewModelScope
                )
            }
        }
    }

    fun stopGeneration() {
        currentModel.value?.let { llmModelHelper.stopResponse(it) }
        inferenceJob?.cancel()
        _isStreaming.value = false
    }
}
