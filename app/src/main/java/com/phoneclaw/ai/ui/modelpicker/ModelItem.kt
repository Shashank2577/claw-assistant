package com.phoneclaw.ai.ui.modelpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoneclaw.ai.data.Model
import com.phoneclaw.ai.data.ModelDownloadStatus
import com.phoneclaw.ai.data.ModelDownloadStatusType.*
import com.phoneclaw.ai.ui.theme.*

@Composable
fun ModelItem(
    model: Model,
    isActive: Boolean,
    downloadStatus: ModelDownloadStatus,
    downloadProgress: Float,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) SurfacePressed else SurfaceCard,
        shadowElevation = if (isActive) 0.dp else 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (downloadStatus.status) {
                                    SUCCEEDED -> AccentGreen
                                    IN_PROGRESS, UNZIPPING -> AccentViolet
                                    FAILED -> AccentRed
                                    else -> ForegroundMuted.copy(alpha = 0.3f)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = ForegroundPrimary
                    )
                }
                
                if (isActive) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Active",
                        tint = AccentViolet,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (downloadStatus.status == NOT_DOWNLOADED) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = "Not Downloaded",
                        tint = ForegroundMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (downloadStatus.status == IN_PROGRESS || downloadStatus.status == UNZIPPING) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = AccentViolet,
                    trackColor = SurfaceCard
                )
            }
        }
    }
}
