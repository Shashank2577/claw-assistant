package com.openclaw.ai.ui.modelpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.openclaw.ai.data.model.*
import com.openclaw.ai.data.model.ModelDownloadStatusType.*
import com.openclaw.ai.ui.theme.OpenClawAITheme

private val PurpleAccent = Color(0xFF7C3AED)
private val PurpleBorder = Color(0xFF7C3AED).copy(alpha = 0.30f)
private val LocalBadgeGreen = Color(0xFF22C55E)
private val CloudBadgeBlue = Color(0xFF0EA5E9)

@Composable
fun ModelItem(
    model: ModelInfo,
    isActive: Boolean,
    downloadStatus: ModelDownloadStatus,
    downloadProgress: Float,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDownloaded = model.isCloud || downloadStatus.status == SUCCEEDED
    val isDownloading = downloadStatus.status == IN_PROGRESS || downloadStatus.status == UNZIPPING

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isActive) {
                    Modifier.border(
                        width = 2.dp,
                        color = PurpleBorder,
                        shape = RoundedCornerShape(16.dp),
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: name + size info + progress
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (model.sizeLabel.isNotEmpty() || model.sizeInBytes > 0L) {
                    val sizeText = buildSizeLabel(model)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!model.isCloud) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "~200ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = PurpleAccent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = PurpleAccent,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: badge + status icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModelTypeBadge(isLocal = model.isLocal)

                Spacer(modifier = Modifier.width(8.dp))

                when {
                    isActive -> {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Active model",
                            tint = PurpleAccent,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    isDownloading -> {
                        // progress is shown inline; no extra icon needed
                    }
                    !isDownloaded -> {
                        IconButton(
                            onClick = onSelect,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = "Download model",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelTypeBadge(isLocal: Boolean) {
    val label = if (isLocal) "Local" else "Cloud"
    val badgeColor = if (isLocal) LocalBadgeGreen else CloudBadgeBlue

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun buildSizeLabel(model: ModelInfo): String {
    val parts = mutableListOf<String>()
    if (model.sizeLabel.isNotEmpty()) parts.add(model.sizeLabel)
    if (model.sizeInBytes > 0L) {
        val gb = model.sizeInBytes / 1_000_000_000.0
        parts.add("%.1f GB".format(gb))
    }
    return parts.joinToString(" · ")
}

@Preview(showBackground = true, name = "ModelItem - Local Downloaded Active")
@Composable
private fun ModelItemLocalActivePreview() {
    OpenClawAITheme {
        ModelItem(
            model = com.openclaw.ai.data.model.DefaultModels.GEMMA_3N_2B,
            isActive = true,
            downloadStatus = ModelDownloadStatus(SUCCEEDED),
            downloadProgress = 0f,
            onSelect = {},
        )
    }
}

@Preview(showBackground = true, name = "ModelItem - Local Downloading")
@Composable
private fun ModelItemDownloadingPreview() {
    OpenClawAITheme {
        ModelItem(
            model = com.openclaw.ai.data.model.DefaultModels.GEMMA_3N_2B,
            isActive = false,
            downloadStatus = ModelDownloadStatus(IN_PROGRESS),
            downloadProgress = 0.63f,
            onSelect = {},
        )
    }
}

@Preview(showBackground = true, name = "ModelItem - Cloud")
@Composable
private fun ModelItemCloudPreview() {
    OpenClawAITheme {
        ModelItem(
            model = com.openclaw.ai.data.model.DefaultModels.GEMINI_FLASH,
            isActive = false,
            downloadStatus = ModelDownloadStatus(SUCCEEDED),
            downloadProgress = 0f,
            onSelect = {},
        )
    }
}
