package com.openclaw.ai.ui.modelpicker

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.openclaw.ai.data.model.*
import com.openclaw.ai.data.model.ModelDownloadStatusType.*

private val PurpleAccent = Color(0xFF7C3AED)
private val SectionLabelGray = Color(0xFF9CA3AF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    onDismiss: () -> Unit,
    onManageModels: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModelPickerViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val localModels by viewModel.localModels.collectAsStateWithLifecycle()
    val cloudModels by viewModel.cloudModels.collectAsStateWithLifecycle()
    val activeModelId by viewModel.activeModelId.collectAsStateWithLifecycle()
    val downloadStatuses by viewModel.downloadStatuses.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
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
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // On-Device section
            if (localModels.isNotEmpty()) {
                SectionLabel(label = "ON-DEVICE MODELS")
                localModels.forEach { model: ModelInfo ->
                    ModelItem(
                        model = model,
                        isActive = model.id == activeModelId,
                        downloadStatus = downloadStatuses[model.id] ?: ModelDownloadStatus(NOT_DOWNLOADED),
                        downloadProgress = downloadProgress[model.id] ?: 0f,
                        onSelect = {
                            viewModel.selectModel(model.id)
                            if (downloadStatuses[model.id]?.status == SUCCEEDED) {
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
                cloudModels.forEach { model: ModelInfo ->
                    ModelItem(
                        model = model,
                        isActive = model.id == activeModelId,
                        downloadStatus = ModelDownloadStatus(SUCCEEDED),
                        downloadProgress = 0f,
                        onSelect = {
                            viewModel.selectModel(model.id)
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
                        color = PurpleAccent,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(20.dp),
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
            letterSpacing = 1.sp,
        ),
        color = SectionLabelGray,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
