package com.phoneclaw.ai.ui.chat

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.util.UUID

private val chatViewJavascriptInterface = ChatWebViewJavascriptInterface()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenDrawer: () -> Unit,
    onOpenModelPicker: () -> Unit,
    onOpenPerChatSettings: (String) -> Unit = {},
    onNavigateToVoice: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val currentModel by viewModel.currentModel.collectAsStateWithLifecycle()
    val conversationTitle by viewModel.conversationTitle.collectAsStateWithLifecycle()

    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

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
                        // Load url.
                        suspendCancellableCoroutine<Unit> { continuation ->
                            chatWebViewClient.setPageLoadListener {
                                chatWebViewClient.setPageLoadListener(null)
                                continuation.resume(Unit)
                            }
                            webViewRef?.loadUrl(action.url)
                        }

                        // Execute JS.
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
                Spacer(modifier = Modifier.height(20.dp))
                ChatTopBar(
                    title = if (conversationTitle.isEmpty()) "New Conversation" else conversationTitle,
                    modelName = currentModel?.displayName ?: "Select Model",
                    onMenuClick = onOpenDrawer,
                    onModelClick = onOpenModelPicker
                )
                QuickActionChipsRow(
                    onAction = { prompt ->
                        viewModel.sendMessage(prompt)
                        focusManager.clearFocus()
                    }
                )
            }
        },
        bottomBar = {
            ClaymorphicInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = { text ->
                    viewModel.sendMessage(text)
                    inputText = ""
                    focusManager.clearFocus()
                },
                onAttach = { /* File picker */ },
                onVoiceToggle = onNavigateToVoice,
                onStop = viewModel::stopGeneration,
                isStreaming = isStreaming,
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            color = MaterialTheme.colorScheme.surface
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

            // Text field — Claymorphic recessed look
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
                    isStreaming -> TrailingAction.STOP
                    text.isNotBlank() -> TrailingAction.SEND
                    else -> TrailingAction.MIC
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "trailing_action",
                modifier = Modifier.align(Alignment.Bottom),
            ) { action ->
                when (action) {
                    TrailingAction.STOP -> FilledIconButton(
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

                    // Send Button with gradient
                    TrailingAction.SEND -> Box(
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

                    // Mic
                    TrailingAction.MIC -> IconButton(
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

private enum class TrailingAction { STOP, SEND, MIC }

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

        Surface(
            onClick = onModelClick,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(36.dp),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(AccentViolet, AccentVioletLight)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
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
}

@Composable
private fun QuickActionChipsRow(onAction: (String) -> Unit) {
    val actions = listOf(
        "Summarize" to AccentViolet,
        "Explain" to AccentPink,
        "Critique" to AccentBlue,
        "Extend" to AccentGreen,
        "Simplify" to AccentAmber
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { (label, color) ->
            Surface(
                onClick = { onAction(label) },
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
