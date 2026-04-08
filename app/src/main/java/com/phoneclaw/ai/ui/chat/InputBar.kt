package com.phoneclaw.ai.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phoneclaw.ai.ui.theme.OpenClawAITheme

/**
 * Input bar fixed at the bottom of the chat screen.
 *
 * Layout: [Attach] [TextField expanding up to 4 lines] [Mic | Send | Stop]
 *
 * - When [text] is empty and not streaming: shows mic button.
 * - When [text] is non-empty and not streaming: shows send button.
 * - When [isStreaming] is true: shows stop button regardless of text.
 *
 * @param text           Current text field value.
 * @param onTextChange   Called when the text field value changes.
 * @param onSend         Called with the current text when the send button is tapped.
 * @param onAttach       Called when the attachment button is tapped.
 * @param onVoiceToggle  Called when the mic button is tapped.
 * @param onStop         Called when the stop button is tapped during streaming.
 * @param isStreaming    True when the assistant is currently generating.
 * @param modifier       Layout modifier applied to the root [Surface].
 */
@Composable
fun InputBar(
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
        color = MaterialTheme.colorScheme.background,
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

            // Text field — no visible border, white surface, pill shape
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Ask me anything...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
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

                    // Purple filled circle with white arrow
                    TrailingAction.SEND -> FilledIconButton(
                        onClick = {
                            if (text.isNotBlank()) onSend(text)
                        },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = "Send message",
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    // Simple unfilled mic icon
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

@Preview(showBackground = true, name = "InputBar - Empty")
@Composable
private fun InputBarEmptyPreview() {
    OpenClawAITheme {
        var text by rememberSaveable { mutableStateOf("") }
        InputBar(
            text = text,
            onTextChange = { text = it },
            onSend = {},
            onAttach = {},
            onVoiceToggle = {},
            onStop = {},
            isStreaming = false,
        )
    }
}

@Preview(showBackground = true, name = "InputBar - With Text")
@Composable
private fun InputBarWithTextPreview() {
    OpenClawAITheme {
        var text by rememberSaveable { mutableStateOf("Hello, world!") }
        InputBar(
            text = text,
            onTextChange = { text = it },
            onSend = {},
            onAttach = {},
            onVoiceToggle = {},
            onStop = {},
            isStreaming = false,
        )
    }
}

@Preview(showBackground = true, name = "InputBar - Streaming")
@Composable
private fun InputBarStreamingPreview() {
    OpenClawAITheme {
        InputBar(
            text = "",
            onTextChange = {},
            onSend = {},
            onAttach = {},
            onVoiceToggle = {},
            onStop = {},
            isStreaming = true,
        )
    }
}
