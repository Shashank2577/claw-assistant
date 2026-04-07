package com.openclaw.ai.ui.modelpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaw.ai.data.Model
import com.openclaw.ai.data.ModelDownloadStatus
import com.openclaw.ai.data.ModelDownloadStatusType.*
import com.openclaw.ai.ui.theme.*

@Composable
fun ModelItem(
    model: Model,
    isActive: Boolean,
    downloadStatus: ModelDownloadStatus,
    downloadProgress: Float,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDownloaded = model.url.isEmpty() || downloadStatus.status == SUCCEEDED
    val isDownloading = downloadStatus.status == IN_PROGRESS || downloadStatus.status == UNZIPPING

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isActive) {
                    Modifier.border(
                        width = 2.dp,
                        color = AccentViolet.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        color = ForegroundInverse,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = ForegroundPrimary,
                )
                
                val sizeLabel = if (model.sizeInBytes > 0) "%.1f GB".format(model.sizeInBytes / 1_000_000_000.0) else ""
                if (sizeLabel.isNotEmpty()) {
                    Text(
                        text = sizeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = ForegroundMuted,
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
                                .clip(CircleShape),
                            color = AccentViolet,
                            trackColor = SurfaceCard,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentViolet,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ModelTypeBadge(isLocal = model.url.isNotEmpty())

                Spacer(modifier = Modifier.width(8.dp))

                if (isActive) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Active",
                        tint = AccentViolet,
                        modifier = Modifier.size(24.dp),
                    )
                } else if (!isDownloaded && !isDownloading) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = "Download",
                        tint = ForegroundMuted,
                        modifier = Modifier.size(24.dp),
                    )
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
