package com.phoneclaw.ai.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phoneclaw.ai.ui.theme.OpenClawAITheme
import com.phoneclaw.ai.ui.theme.customColors

private data class QuickAction(
    val label: String,
    val prompt: String,
    val icon: ImageVector,
)

private val QUICK_ACTIONS = listOf(
    QuickAction(
        label = "Summarize",
        prompt = "Summarize the conversation so far.",
        icon = Icons.Outlined.AutoAwesome,
    ),
    QuickAction(
        label = "Explain",
        prompt = "Explain this in simple terms.",
        icon = Icons.Outlined.School,
    ),
    QuickAction(
        label = "Critique",
        prompt = "Critique what has been discussed and point out any weaknesses.",
        icon = Icons.Outlined.RateReview,
    ),
    QuickAction(
        label = "Extend",
        prompt = "Extend or elaborate on the last response.",
        icon = Icons.Outlined.AddCircle,
    ),
    QuickAction(
        label = "Simplify",
        prompt = "Simplify the last response.",
        icon = Icons.Outlined.UnfoldLess,
    ),
)

private val ChipShape = RoundedCornerShape(20.dp)

/**
 * Horizontally scrollable row of quick-action suggestion chips shown below the top bar.
 * Each chip has a purple gradient background with white text and icon.
 *
 * @param onAction  Called with the full prompt string when a chip is tapped.
 * @param modifier  Layout modifier applied to the [LazyRow].
 */
@Composable
fun QuickActionChips(
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradientStart = MaterialTheme.customColors.chipGradientStart
    val gradientEnd = MaterialTheme.customColors.chipGradientEnd

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        items(QUICK_ACTIONS) { action ->
            GradientChip(
                label = action.label,
                icon = action.icon,
                gradientStart = gradientStart,
                gradientEnd = gradientEnd,
                onClick = { onAction(action.prompt) },
            )
        }
    }
}

@Composable
private fun GradientChip(
    label: String,
    icon: ImageVector,
    gradientStart: Color,
    gradientEnd: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(
                brush = Brush.horizontalGradient(listOf(gradientStart, gradientEnd)),
                shape = ChipShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

@Preview(showBackground = true, name = "QuickActionChips")
@Composable
private fun QuickActionChipsPreview() {
    OpenClawAITheme {
        QuickActionChips(onAction = {})
    }
}
