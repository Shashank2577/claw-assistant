package com.phoneclaw.ai.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.phoneclaw.ai.data.model.ChatMessageData
import com.phoneclaw.ai.data.model.MessageRole
import com.phoneclaw.ai.ui.common.MarkdownText
import com.phoneclaw.ai.ui.theme.*

@Composable
fun MessageBubble(
    message: ChatMessageData,
    isStreaming: Boolean = false,
    onImageClick: (String) -> Unit = {},
) {
    val isUser = message.role == MessageRole.USER

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        if (isUser) {
            UserBubble(message, onImageClick)
        } else {
            AssistantBubble(message, isStreaming)
        }
    }
}

@Composable
private fun UserBubble(
    message: ChatMessageData,
    onImageClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!message.mediaUri.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(message.mediaUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Attached image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onImageClick(message.mediaUri) },
                error = {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(SurfaceCard, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.BrokenImage, null, tint = ForegroundMuted)
                    }
                },
            )
        }

        if (message.content.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp),
                color = AccentViolet,
                shadowElevation = 1.dp,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun AssistantBubble(
    message: ChatMessageData,
    isStreaming: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 48.dp, top = 4.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Thinking Section
        if (!message.thought.isNullOrBlank()) {
            ThinkingBubble(thought = message.thought)
        }

        // Webview result (for interactive map, etc.)
        if (message.webviewUrl != null) {
            MessageBodyWebview(message = message)
        }

        // Main Response
        if (message.content.isNotBlank()) {
            MarkdownText(
                text = message.content,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (isStreaming && message.content.isEmpty()) {
            StreamingIndicator()
        }
    }
}

@Composable
private fun ThinkingBubble(thought: String) {
    Surface(
        shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
        color = SurfaceCard,
        modifier = Modifier.widthIn(max = 320.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = AccentViolet,
                modifier = Modifier.size(16.dp).padding(top = 2.dp)
            )
            Text(
                text = thought,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 20.sp,
                    fontSize = 14.sp
                ),
                color = ForegroundSecondary
            )
        }
    }
}

@Composable
private fun StreamingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "streaming")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(AccentViolet.copy(alpha = alpha), RoundedCornerShape(4.dp)),
    )
}
