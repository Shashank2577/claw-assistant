package com.phoneclaw.ai.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phoneclaw.ai.ui.common.MarkdownText
import com.phoneclaw.ai.ui.common.RotationalLoader
import com.phoneclaw.ai.ui.theme.OpenClawAITheme
import com.phoneclaw.ai.ui.theme.customColors

/**
 * Left-aligned thinking indicator shown while the assistant is generating.
 *
 * Displays the [RotationalLoader] alongside "Thinking..." text.  When
 * [thinkingText] is non-empty the chain-of-thought can be expanded/collapsed
 * with an animated toggle.
 *
 * @param thinkingText  The model's chain-of-thought content.  Pass empty string
 *                      while no thinking content has arrived yet.
 */
@Composable
fun ThinkingIndicator(
    thinkingText: String = "",
    size: Dp = 32.dp,
    dotSize: Dp = 8.dp,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(start = 12.dp, end = 48.dp, top = 8.dp, bottom = 4.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Header row: loader + label + expand toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (thinkingText.isNotEmpty()) {
                Modifier
                    .clickable { expanded = !expanded }
                    .padding(4.dp)
            } else {
                Modifier.padding(4.dp)
            },
        ) {
            RotationalLoader(
                text = "",
                dotSize = dotSize,
                modifier = Modifier.size(size),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Thinking...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.customColors.sectionLabel,
            )

            if (thinkingText.isNotEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.KeyboardArrowUp
                    } else {
                        Icons.Outlined.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) "Collapse thinking" else "Expand thinking",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Expandable thinking content
        AnimatedVisibility(
            visible = expanded && thinkingText.isNotEmpty(),
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(200)),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MarkdownText(
                    text = thinkingText,
                    smallFontSize = true,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "ThinkingIndicator - No Content")
@Composable
private fun ThinkingIndicatorEmptyPreview() {
    OpenClawAITheme {
        ThinkingIndicator()
    }
}

@Preview(showBackground = true, name = "ThinkingIndicator - With Content")
@Composable
private fun ThinkingIndicatorWithContentPreview() {
    OpenClawAITheme {
        ThinkingIndicator(thinkingText = "Let me think step by step about this problem...")
    }
}
