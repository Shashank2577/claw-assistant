package com.phoneclaw.ai.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phoneclaw.ai.ui.common.MarkdownText
import com.phoneclaw.ai.ui.theme.OpenClawAITheme
import com.phoneclaw.ai.ui.theme.customColors

/**
 * Card that visualises a tool invocation and its result.
 *
 * @param toolName   Name of the tool that was called.
 * @param params     Optional JSON string of the parameters passed to the tool.
 * @param result     Optional result string returned by the tool.
 * @param isLoading  True while the tool call is in progress.
 */
@Composable
fun ToolResultCard(
    toolName: String,
    params: String? = null,
    result: String? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var paramsExpanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        // Row with IntrinsicSize.Min height so the accent bar fills the card height
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left purple accent border (4dp)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        color = MaterialTheme.customColors.toolResultBorder,
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                    ),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Header: tool icon + tool name + spinner or green checkmark
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = toolName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Complete",
                            tint = MaterialTheme.customColors.localBadgeGreen,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                // Collapsible params section
                if (!params.isNullOrBlank()) {
                    HorizontalDivider(color = MaterialTheme.customColors.toolResultBorder)

                    TextButton(
                        onClick = { paramsExpanded = !paramsExpanded },
                        modifier = Modifier.padding(0.dp),
                    ) {
                        Text(
                            text = if (paramsExpanded) "Hide parameters" else "Show parameters",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (paramsExpanded) {
                                Icons.Outlined.KeyboardArrowUp
                            } else {
                                Icons.Outlined.KeyboardArrowDown
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                    }

                    AnimatedVisibility(
                        visible = paramsExpanded,
                        enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
                    ) {
                        MarkdownText(
                            text = "```json\n$params\n```",
                            smallFontSize = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Result body
                if (!isLoading) {
                    HorizontalDivider(color = MaterialTheme.customColors.toolResultBorder)
                    if (!result.isNullOrBlank()) {
                        MarkdownText(
                            text = result,
                            smallFontSize = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            text = "No result",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.padding(top = 4.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = "Running tool...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "ToolResultCard - Loading")
@Composable
private fun ToolResultCardLoadingPreview() {
    OpenClawAITheme {
        ToolResultCard(
            toolName = "web_search",
            params = """{"query": "Kotlin Jetpack Compose"}""",
            isLoading = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "ToolResultCard - With Result")
@Composable
private fun ToolResultCardResultPreview() {
    OpenClawAITheme {
        ToolResultCard(
            toolName = "web_search",
            params = """{"query": "Kotlin Jetpack Compose"}""",
            result = "Jetpack Compose is Android's modern toolkit for building native UI.",
            isLoading = false,
            modifier = Modifier.padding(16.dp),
        )
    }
}
