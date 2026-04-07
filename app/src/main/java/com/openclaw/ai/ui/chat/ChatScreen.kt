package com.openclaw.ai.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openclaw.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenDrawer: () -> Unit,
    onOpenModelPicker: () -> Unit,
    onOpenPerChatSettings: () -> Unit = {},
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

    Scaffold(
        containerColor = CanvasBg,
        topBar = {
            Column(modifier = Modifier.background(CanvasBg)) {
                // Status Bar Spacer
                Spacer(modifier = Modifier.height(20.dp))
                
                ChatTopBar(
                    title = if (conversationTitle.isEmpty()) "New Conversation" else conversationTitle,
                    modelName = currentModel?.displayName ?: "Select Model",
                    onMenuClick = onOpenDrawer,
                    onModelClick = onOpenModelPicker
                )

                // Quick Action Chips Row
                QuickActionChipsRow(
                    onAction = { prompt ->
                        viewModel.sendMessage(prompt)
                        focusManager.clearFocus()
                    }
                )
            }
        },
        bottomBar = {
            InputBar(
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            if (messages.isEmpty() && !isStreaming) {
                EmptyState(onSuggestionClick = { viewModel.sendMessage(it) })
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
        }
    }
}

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
        // Menu Button (Neumorphic Card Style)
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

        // Title
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

        // Model Chip (Gradient)
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

@Composable
private fun EmptyState(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "How can I help you today?",
            style = MaterialTheme.typography.headlineMedium,
            color = ForegroundPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "PocketAI is ready to assist with any task.",
            style = MaterialTheme.typography.bodyMedium,
            color = ForegroundMuted
        )
    }
}
