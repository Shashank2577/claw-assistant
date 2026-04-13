package com.phoneclaw.ai.ui.modelpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneclaw.ai.data.Model
import com.phoneclaw.ai.data.ModelDownloadStatus
import com.phoneclaw.ai.data.ModelDownloadStatusType.*
import com.phoneclaw.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    onDismiss: () -> Unit,
    onManageModels: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModelPickerViewModel = hiltViewModel(),
) {
    val localModels by viewModel.localModels.collectAsStateWithLifecycle()
    val cloudModels by viewModel.cloudModels.collectAsStateWithLifecycle()
    val activeModelId by viewModel.activeModelId.collectAsStateWithLifecycle()
    val downloadStatuses by viewModel.downloadStatuses.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = ClayCardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            // Header
            Text(
                text = "Select Model",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = NunitoFontFamily
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = ForegroundPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // On-Device section
            if (localModels.isNotEmpty()) {
                SectionLabel(label = "ON-DEVICE MODELS")
                localModels.forEach { model: Model ->
                    val status = downloadStatuses[model.name] ?: ModelDownloadStatus(status = NOT_DOWNLOADED)
                    ClayModelItem(
                        model = model,
                        isActive = model.name == activeModelId,
                        downloadStatus = status,
                        downloadProgress = downloadProgress[model.name] ?: 0f,
                        onSelect = {
                            if (status.status == SUCCEEDED) {
                                viewModel.selectModel(model.name)
                                onDismiss()
                            }
                        },
                        onDownload = { viewModel.downloadModel(model) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Cloud section
            if (cloudModels.isNotEmpty()) {
                SectionLabel(label = "CLOUD MODELS")
                cloudModels.forEach { model: Model ->
                    ClayModelItem(
                        model = model,
                        isActive = model.name == activeModelId,
                        downloadStatus = ModelDownloadStatus(status = SUCCEEDED),
                        downloadProgress = 0f,
                        onSelect = {
                            viewModel.selectModel(model.name)
                            onDismiss()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer - Manage models
            Surface(
                onClick = onManageModels,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))
                            )
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Manage & Download Models",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClayModelItem(
    model: Model,
    isActive: Boolean,
    downloadStatus: ModelDownloadStatus,
    downloadProgress: Float,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownloadable = downloadStatus.status == NOT_DOWNLOADED || downloadStatus.status == FAILED
    Surface(
        onClick = if (isDownloadable) onDownload else onSelect,
        shape = RoundedCornerShape(32.dp),
        color = if (isActive) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.8f),
        shadowElevation = 0.dp,
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
                    Column {
                        Text(
                            text = model.displayName.ifEmpty { model.name },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = NunitoFontFamily
                            ),
                            color = ForegroundPrimary
                        )
                        if (isDownloadable) {
                            Text(
                                text = "Tap to download",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentViolet,
                            )
                        }
                    }
                }

                when {
                    isActive -> Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Active",
                        tint = AccentViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    isDownloadable -> Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = "Download",
                        tint = AccentViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    else -> {}
                }
            }

            if (downloadStatus.status == IN_PROGRESS || downloadStatus.status == UNZIPPING) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = AccentViolet,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        ),
        color = ForegroundMuted,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
