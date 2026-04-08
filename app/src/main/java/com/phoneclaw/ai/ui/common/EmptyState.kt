package com.phoneclaw.ai.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phoneclaw.ai.ui.theme.OpenClawAITheme

/**
 * Reusable empty-state composable.
 *
 * Used in: empty chat (welcome message), empty file browser, empty conversation
 * list. Centre-aligned with an icon, headline title, body subtitle, and an
 * optional action button.
 *
 * @param icon         Vector icon displayed at the top (56 dp).
 * @param title        Primary headline (headlineSmall).
 * @param subtitle     Secondary description (bodyMedium).
 * @param actionLabel  Label for the optional CTA button. Null hides the button.
 * @param onAction     Click handler for the CTA button.
 * @param modifier     Layout modifier applied to the root [Column].
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

@Preview(showBackground = true, name = "EmptyState - With Action")
@Composable
private fun EmptyStateWithActionPreview() {
    OpenClawAITheme {
        EmptyState(
            icon = Icons.Outlined.ChatBubbleOutline,
            title = "No conversations yet",
            subtitle = "Start a new chat to begin talking with an AI model on your device.",
            actionLabel = "New Chat",
            onAction = {},
            modifier = Modifier.padding(vertical = 48.dp),
        )
    }
}

@Preview(showBackground = true, name = "EmptyState - No Action")
@Composable
private fun EmptyStateNoActionPreview() {
    OpenClawAITheme {
        EmptyState(
            icon = Icons.Outlined.ChatBubbleOutline,
            title = "Nothing here",
            subtitle = "Files you attach will appear here.",
            modifier = Modifier.padding(vertical = 48.dp),
        )
    }
}
