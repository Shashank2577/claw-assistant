package com.phoneclaw.ai.ui.modelpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        containerColor = CanvasBg,
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
                    ModelItem(
                        model = model,
                        isActive = model.name == activeModelId,
                        downloadStatus = downloadStatuses[model.name] ?: ModelDownloadStatus(status = NOT_DOWNLOADED),
                        downloadProgress = downloadProgress[model.name] ?: 0f,
                        onSelect = {
                            viewModel.selectModel(model.name)
                            if (downloadStatuses[model.name]?.status == SUCCEEDED) {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Cloud section
            if (cloudModels.isNotEmpty()) {
                SectionLabel(label = "CLOUD MODELS")
                cloudModels.forEach { model: Model ->
                    ModelItem(
                        model = model,
                        isActive = model.name == activeModelId,
                        downloadStatus = ModelDownloadStatus(status = SUCCEEDED),
                        downloadProgress = 0f,
                        onSelect = {
                            viewModel.selectModel(model.name)
                            onDismiss()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer - Manage models
            TextButton(
                onClick = onManageModels,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Manage & Download Models",
                        color = AccentViolet,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = AccentViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
