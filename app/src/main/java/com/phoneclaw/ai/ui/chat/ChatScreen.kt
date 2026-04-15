package com.phoneclaw.ai.ui.chat

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneclaw.ai.common.AskInfoAgentAction
import com.phoneclaw.ai.common.CallJsAgentAction
import com.phoneclaw.ai.common.SkillProgressAgentAction
import com.phoneclaw.ai.ui.common.BaseOpenClawWebViewClient
import com.phoneclaw.ai.ui.common.EmptyState
import com.phoneclaw.ai.ui.common.OpenClawWebView
import com.phoneclaw.ai.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private val chatViewJavascriptInterface = ChatWebViewJavascriptInterface()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenDrawer: () -> Unit,
    onOpenModelPicker: () -> Unit,
    onOpenPerChatSettings: (String) -> Unit = {},
    onNavigateToVoice: () -> Unit = {},
    initialMode: ChatMode = ChatMode.CHAT,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    // Initialize mode and preload model
    LaunchedEffect(initialMode) { viewModel.setChatMode(initialMode) }
    LaunchedEffect(Unit) { viewModel.preloadModel() }
    val currentModelForEffect by viewModel.currentModel.collectAsStateWithLifecycle()
    LaunchedEffect(currentModelForEffect) { if (currentModelForEffect != null) viewModel.preloadModel() }
    val chatModeForEffect by viewModel.chatMode.collectAsStateWithLifecycle()
    LaunchedEffect(chatModeForEffect) { viewModel.preloadModel() }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val currentModel by viewModel.currentModel.collectAsStateWithLifecycle()
    val conversationTitle by viewModel.conversationTitle.collectAsStateWithLifecycle()
    val chatMode by viewModel.chatMode.collectAsStateWithLifecycle()
    val modelInitState by viewModel.modelInitState.collectAsStateWithLifecycle()
    val attachedImageBitmap by viewModel.attachedImageBitmap.collectAsStateWithLifecycle()
    val attachedImageUri by viewModel.attachedImageUri.collectAsStateWithLifecycle()

    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Prompt Lab state
    var selectedPromptTemplate by remember { mutableStateOf(PromptTemplateType.FREE_FORM) }
    var selectedPromptOption by remember { mutableStateOf("") }

    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) viewModel.attachImage(bitmap, it.toString())
            }
        }
    }

    // Agent Skills State
    var showAskInfoDialog by remember { mutableStateOf(false) }
    var currentAskInfoAction by remember { mutableStateOf<AskInfoAgentAction?>(null) }
    var askInfoInputValue by remember { mutableStateOf("") }
    var webViewRef: WebView? by remember { mutableStateOf(null) }
    val chatWebViewClient = remember { ChatWebViewClient(context = context) }

    // Handle Agent Actions
    val actionChannel = viewModel.agentTools.actionChannel
    LaunchedEffect(actionChannel) {
        for (action in actionChannel) {
            when (action) {
                is CallJsAgentAction -> {
                    try {
                        suspendCancellableCoroutine<Unit> { continuation ->
                            chatWebViewClient.setPageLoadListener {
                                chatWebViewClient.setPageLoadListener(null)
                                continuation.resume(Unit)
                            }
                            webViewRef?.loadUrl(action.url)
                        }

                        chatViewJavascriptInterface.onResultListener = { result ->
                            action.result.complete(result)
                        }

                        val script = """
                          (async function() {
                              var startTs = Date.now();
                              while(true) {
                                if (typeof openclaw_get_result === 'function') {
                                  break;
                                }
                                await new Promise(resolve=>{
                                  setTimeout(resolve, 100)
                                });
                                if (Date.now() - startTs > 10000) {
                                  break;
                                }
                              }
                              var result = await openclaw_get_result(`${action.data}`, `${action.secret}`);
                              OpenClawBridge.onResultReady(result);
                          })()
                        """.trimIndent()
                        webViewRef?.evaluateJavascript(script, null)
                    } catch (e: Exception) {
                        action.result.completeExceptionally(e)
                    }
                }
                is AskInfoAgentAction -> {
                    currentAskInfoAction = action
                    askInfoInputValue = ""
                    showAskInfoDialog = true
                }
                is SkillProgressAgentAction -> {
                    // Update progress in UI if needed
                }
            }
        }
    }

    Scaffold(
        containerColor = CanvasBg,
        topBar = {
            Column(modifier = Modifier.background(CanvasBg)) {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                ChatTopBar(
                    title = if (conversationTitle.isEmpty()) "New Conversation" else conversationTitle,
                    modelName = currentModel?.displayName ?: "Select Model",
                    onMenuClick = onOpenDrawer,
                    onModelClick = onOpenModelPicker
                )
                // Mode-conditional content below top bar
                when (chatMode) {
                    ChatMode.CHAT, ChatMode.ASK_IMAGE -> QuickActionChipsRow(
                        onAction = { prompt ->
                            viewModel.sendMessage(prompt)
                            focusManager.clearFocus()
                        }
                    )
                    ChatMode.PROMPT_LAB -> {
                        val template = PROMPT_TEMPLATES.first { it.type == selectedPromptTemplate }
                        PromptLabTemplateBar(selectedPromptTemplate) {
                            selectedPromptTemplate = it
                            selectedPromptOption = ""
                        }
                        if (template.options.isNotEmpty()) {
                            PromptLabOptionsRow(template, selectedPromptOption) { selectedPromptOption = it }
                        }
                    }
                    ChatMode.AGENT -> {
                        AgentSkillChipsSection(
                            onChipClick = { prompt ->
                                viewModel.sendMessage(prompt)
                                focusManager.clearFocus()
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column {
                // Mode switcher
                ModeSwitcherRow(chatMode) { viewModel.setChatMode(it) }

                // Image attachment preview
                if (attachedImageUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        coil.compose.AsyncImage(
                            model = attachedImageUri,
                            contentDescription = "Attached image",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        IconButton(onClick = { viewModel.clearAttachment() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove attachment")
                        }
                        Text("Image attached", style = MaterialTheme.typography.bodySmall, color = ForegroundSecondary)
                    }
                }

                ClaymorphicInputBar(
                    text = inputText,
                    onTextChange = { inputText = it },
                    onSend = { rawInput ->
                        val finalText = if (chatMode == ChatMode.PROMPT_LAB) {
                            val template = PROMPT_TEMPLATES.first { it.type == selectedPromptTemplate }
                            buildPrompt(template, selectedPromptOption, rawInput)
                        } else rawInput
                        val images = listOfNotNull(attachedImageBitmap)
                        viewModel.sendMessage(finalText, images)
                        inputText = ""
                        focusManager.clearFocus()
                    },
                    onAttach = { imagePicker.launch("image/*") },
                    onVoiceToggle = onNavigateToVoice,
                    onStop = viewModel::stopGeneration,
                    isStreaming = isStreaming,
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            color = CanvasBg
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (messages.isEmpty() && !isStreaming) {
                    EmptyState(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "How can I help you today?",
                        subtitle = "PocketAI is ready to assist with any task.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                isStreaming = isStreaming && message.isStreaming
                            )
                        }
                    }
                }

                // Loading overlay
                AnimatedVisibility(
                    visible = modelInitState is ModelInitState.Loading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CanvasBg.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = SurfaceVioletCard,
                            shadowElevation = 8.dp,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(horizontal = 40.dp, vertical = 32.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = AccentViolet,
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 3.dp,
                                )
                                Text(
                                    "Loading model...",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = ForegroundPrimary,
                                )
                                Text(
                                    "This may take a moment",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ForegroundSecondary,
                                )
                            }
                        }
                    }
                }

                // Hidden WebView for JS skills
                Box(modifier = Modifier.size(1.dp).clip(CircleShape)) {
                    OpenClawWebView(
                        onWebViewCreated = { webView ->
                            webViewRef = webView
                            webView.addJavascriptInterface(chatViewJavascriptInterface, "OpenClawBridge")
                        },
                        customWebViewClient = chatWebViewClient
                    )
                }
            }
        }
    }

    if (showAskInfoDialog && currentAskInfoAction != null) {
        val action = currentAskInfoAction!!
        SecretEditorDialog(
            title = action.dialogTitle,
            fieldLabel = action.fieldLabel,
            value = askInfoInputValue,
            onValueChange = { askInfoInputValue = it },
            onDone = {
                action.result.complete(askInfoInputValue)
                showAskInfoDialog = false
                currentAskInfoAction = null
            },
            onDismiss = {
                action.result.complete("")
                showAskInfoDialog = false
                currentAskInfoAction = null
            }
        )
    }
}

@Composable
private fun ModeSwitcherRow(
    currentMode: ChatMode,
    onModeChange: (ChatMode) -> Unit,
) {
    val modes = listOf(
        Triple(ChatMode.CHAT, "Chat", Icons.Rounded.AutoAwesome),
        Triple(ChatMode.ASK_IMAGE, "Image", Icons.Outlined.Image),
        Triple(ChatMode.PROMPT_LAB, "Prompt Lab", Icons.Outlined.Science),
        Triple(ChatMode.AGENT, "Agent", Icons.Outlined.Psychology),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CanvasBg)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        modes.forEach { (mode, label, icon) ->
            FilterChip(
                selected = currentMode == mode,
                onClick = { onModeChange(mode) },
                label = { Text(label, fontSize = 12.sp) },
                leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentViolet,
                    selectedLabelColor = ForegroundInverse,
                    selectedLeadingIconColor = ForegroundInverse,
                    containerColor = SurfaceCard,
                    labelColor = ForegroundSecondary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = currentMode == mode,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun AgentSkillChipsSection(onChipClick: (String) -> Unit) {
    val tryOutChips = listOf(
        "Interactive Map" to "Show me Googleplex on interactive map",
        "Kitchen Adventure" to "Start kitchen adventure",
        "Calculate Hash" to "What is the sha1 hash of 'gemma'?",
        "Text Spinner" to "Spin 'Gemma' on my head",
        "Send Email" to "Send email 'Good morning' to abc@example.com with subject 'Hello'",
        "Track Mood" to "Track my mood today",
        "Wikipedia" to "Check Wikipedia about Oscars 2026",
        "QR Code" to "Generate QR code for https://gemma.google.com",
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tryOutChips.forEach { (label, prompt) ->
                Surface(
                    onClick = { onChipClick(prompt) },
                    shape = RoundedCornerShape(24.dp),
                    color = AccentPink,
                    modifier = Modifier.height(32.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp), color = ForegroundInverse)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Info, null, tint = ForegroundMuted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Agent mode uses AI skills. Skills must be downloaded to work.", style = MaterialTheme.typography.bodySmall, color = ForegroundMuted)
        }
    }
}

@Composable
private fun ClaymorphicInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onAttach: () -> Unit,
    onVoiceToggle: () -> Unit,
    onStop: () -> Unit,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Attachment button
            IconButton(
                onClick = onAttach,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .align(Alignment.Bottom),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = "Attach file",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Text field
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Ask me anything...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFEFEBF5),
                    unfocusedContainerColor = Color(0xFFEFEBF5),
                ),
            )

            // Trailing action button: Stop / Send / Mic
            AnimatedContent(
                targetState = when {
                    isStreaming -> ClaymorphicTrailingAction.STOP
                    text.isNotBlank() -> ClaymorphicTrailingAction.SEND
                    else -> ClaymorphicTrailingAction.MIC
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "trailing_action",
                modifier = Modifier.align(Alignment.Bottom),
            ) { action ->
                when (action) {
                    ClaymorphicTrailingAction.STOP -> FilledIconButton(
                        onClick = onStop,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.StopCircle,
                            contentDescription = "Stop generation",
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    ClaymorphicTrailingAction.SEND -> Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))))
                            .clickable { if (text.isNotBlank()) onSend(text) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = "Send message",
                            modifier = Modifier.size(22.dp),
                            tint = Color.White
                        )
                    }

                    ClaymorphicTrailingAction.MIC -> IconButton(
                        onClick = onVoiceToggle,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = "Voice input",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private enum class ClaymorphicTrailingAction { STOP, SEND, MIC }

@Composable
private fun ChatTopBar(
    title: String,
    modelName: String,
    onMenuClick: () -> Unit,
    onModelClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            onClick = onMenuClick,
            shape = RoundedCornerShape(12.dp),
            color = SurfaceCard,
            shadowElevation = 2.dp,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Menu",
                    tint = ForegroundPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = NunitoFontFamily,
                fontSize = 18.sp
            ),
            color = ForegroundPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        )

        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(AccentViolet, AccentVioletLight)
                    )
                )
                .clickable(onClick = onModelClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = ForegroundInverse,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = ForegroundInverse
                )
            }
    }
}

@Composable
private fun QuickActionChipsRow(onAction: (String) -> Unit) {
    val actions = listOf(
        Triple("Summarize", "Please summarize our conversation so far in a few bullet points.", AccentViolet),
        Triple("Explain", "Please explain the key concepts in your last response in simple, easy-to-understand terms.", AccentPink),
        Triple("Critique", "Please critically analyze your last response and identify any weaknesses or gaps.", AccentBlue),
        Triple("Extend", "Please elaborate further and provide more detail on what you just said.", AccentGreen),
        Triple("Simplify", "Please rewrite your last response in plain, simple language as if explaining to a beginner.", AccentAmber)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { (label, prompt, color) ->
            Surface(
                onClick = { onAction(prompt) },
                shape = RoundedCornerShape(24.dp),
                color = color,
                modifier = Modifier.height(32.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = ForegroundInverse
                    )
                }
            }
        }
    }
}

class ChatWebViewJavascriptInterface {
    var onResultListener: ((String) -> Unit)? = null

    @JavascriptInterface
    fun onResultReady(result: String) {
        onResultListener?.invoke(result)
    }
}

class ChatWebViewClient(val context: Context) : BaseOpenClawWebViewClient(context = context) {
    private var onPageLoaded: (() -> Unit)? = null

    fun setPageLoadListener(listener: (() -> Unit)?) {
        onPageLoaded = listener
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageLoaded?.invoke()
    }
}
