package com.phoneclaw.ai.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

enum class ChatMode { CHAT, ASK_IMAGE, PROMPT_LAB, AGENT }

sealed interface ModelInitState {
    data object Idle : ModelInitState
    data object Loading : ModelInitState
    data object Ready : ModelInitState
    data class Error(val message: String) : ModelInitState
}

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

    private val _chatMode = MutableStateFlow(ChatMode.CHAT)
    val chatMode: StateFlow<ChatMode> = _chatMode.asStateFlow()

    private val _modelInitState = MutableStateFlow<ModelInitState>(ModelInitState.Idle)
    val modelInitState: StateFlow<ModelInitState> = _modelInitState.asStateFlow()

    private val _attachedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val attachedImageBitmap: StateFlow<Bitmap?> = _attachedImageBitmap.asStateFlow()

    private val _attachedImageUri = MutableStateFlow<String?>(null)
    val attachedImageUri: StateFlow<String?> = _attachedImageUri.asStateFlow()

    /** When true, tools and constrained decoding are always enabled regardless of skill selection. */
    val agentMode: Boolean get() = _chatMode.value == ChatMode.AGENT

    init {
        agentTools.context = context
        agentTools.skillManagerViewModel = skillManagerViewModel

        viewModelScope.launch {
            skillManagerViewModel.loadSkills {}
        }
    }

    fun setChatMode(mode: ChatMode) {
        _chatMode.value = mode
        _messages.value = emptyList()
        _attachedImageBitmap.value = null
        _attachedImageUri.value = null
        // Force model re-initialization on every mode switch so that tool-calling
        // config (enabled in AGENT mode, disabled in CHAT/PROMPT_LAB) is applied.
        _modelInitialized = false
        _modelInitState.value = ModelInitState.Idle
    }

    fun attachImage(bitmap: Bitmap, uri: String) {
        _attachedImageBitmap.value = bitmap
        _attachedImageUri.value = uri
    }

    fun clearAttachment() {
        _attachedImageBitmap.value = null
        _attachedImageUri.value = null
    }

    fun preloadModel() {
        val model = currentModel.value ?: return
        val needsInit = !_modelInitialized || _initializedModelId != model.name
        if (!needsInit) {
            _modelInitState.value = ModelInitState.Ready
            return
        }
        if (_modelInitState.value is ModelInitState.Loading) return

        _modelInitState.value = ModelInitState.Loading

        val selectedSkills = skillManagerViewModel.getSelectedSkills()
        val useTools = agentMode || selectedSkills.isNotEmpty()

        llmModelHelper.initialize(
            context = context,
            model = model,
            supportImage = model.llmSupportImage,
            supportAudio = model.llmSupportAudio,
            onDone = { error ->
                if (error.isNotEmpty()) {
                    _modelInitState.value = ModelInitState.Error(error)
                    return@initialize
                }
                _modelInitialized = true
                _initializedModelId = model.name
                _modelInitState.value = ModelInitState.Ready
            },
            systemInstruction = if (useTools) skillManagerViewModel.getSystemPrompt("You are a helpful AI assistant with access to tools.") else null,
            tools = if (useTools) listOf(tool(agentTools)) else emptyList(),
            enableConversationConstrainedDecoding = useTools,
            coroutineScope = viewModelScope
        )
    }

    private suspend fun startNewConversation() {
        val id = conversationRepository.createConversation("default", "New Chat")
        _currentConversationId.value = id
    }

    fun sendMessage(text: String, images: List<Bitmap> = emptyList()) {
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
                mediaUri = _attachedImageUri.value,
                timestamp = userTimestamp
            )
            _messages.update { it + userMsg }
            _attachedImageUri.value = null
            _attachedImageBitmap.value = null

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
            val useTools = agentMode || selectedSkills.isNotEmpty()

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
                            _modelInitState.value = ModelInitState.Error(error)
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
                        _modelInitState.value = ModelInitState.Ready

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
                            images = images,
                            coroutineScope = viewModelScope
                        )
                    },
                    systemInstruction = if (useTools) skillManagerViewModel.getSystemPrompt("You are a helpful AI assistant with access to tools.") else null,
                    tools = if (useTools) listOf(tool(agentTools)) else emptyList(),
                    enableConversationConstrainedDecoding = useTools,
                    coroutineScope = viewModelScope
                )
            } else {
                // In PROMPT_LAB mode, reset conversation before each inference
                if (_chatMode.value == ChatMode.PROMPT_LAB) {
                    llmModelHelper.resetConversation(model, model.llmSupportImage, model.llmSupportAudio)
                }

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
                    images = images,
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
