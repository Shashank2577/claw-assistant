package com.openclaw.ai.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openclaw.ai.data.*
import com.openclaw.ai.data.ModelDownloadStatusType.*
import com.openclaw.ai.data.repository.ThemeMode
import com.openclaw.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val downloadStatuses by viewModel.downloadStatuses.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val defaultModelId by viewModel.defaultModelId.collectAsStateWithLifecycle()

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = CanvasBg,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = NunitoFontFamily
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasBg)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Appearance
            item {
                SectionHeader("Appearance")
                SettingsCard {
                    ThemeSelector(
                        selected = themeMode,
                        onSelect = viewModel::setTheme
                    )
                }
            }

            // Models Management
            item {
                SectionHeader("On-Device Models")
            }
            
            val localModels = availableModels.filter { it.url.isNotEmpty() }
            items(localModels, key = { it.name }) { model ->
                ModelManagementCard(
                    model = model,
                    status = downloadStatuses[model.name] ?: ModelDownloadStatus(NOT_DOWNLOADED),
                    progress = downloadProgress[model.name] ?: 0f,
                    isDefault = model.name == defaultModelId,
                    onDownload = { viewModel.downloadModel(model.name) },
                    onDelete = { viewModel.deleteModel(model.name) },
                    onSetDefault = { viewModel.setDefaultModel(model.name) },
                    onCancel = { viewModel.cancelDownload(model.name) }
                )
            }

            // Cloud Models / API Key
            item {
                SectionHeader("Cloud Models")
                SettingsCard {
                    ApiKeySection(
                        apiKey = uiState.geminiApiKey,
                        isKeyVisible = uiState.isApiKeyVisible,
                        onApiKeyChange = viewModel::onApiKeyChange,
                        onToggleVisibility = viewModel::toggleApiKeyVisibility,
                        onSave = viewModel::saveApiKey,
                        isValid = uiState.isApiKeyValid,
                        isTesting = uiState.isTesting
                    )
                }
            }

            // Storage & Data
            item {
                SectionHeader("Storage & Data")
                SettingsCard {
                    StorageActionItem(
                        icon = Icons.Rounded.DeleteSweep,
                        label = "Clear All Conversations",
                        color = AccentRed,
                        onClick = { showClearDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderMuted)
                    StorageActionItem(
                        icon = Icons.Rounded.IosShare,
                        label = "Export Conversations",
                        onClick = { /* Export logic */ }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Data?") },
            text = { Text("This will permanently delete all your conversation history. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllConversations()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AccentRed)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = ForegroundMuted
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ForegroundInverse,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ModelManagementCard(
    model: Model,
    status: ModelDownloadStatus,
    progress: Float,
    isDefault: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    onCancel: () -> Unit
) {
    SettingsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        model.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ForegroundPrimary
                    )
                    if (isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = AccentGreen) { Text("DEFAULT", fontSize = 9.sp) }
                    }
                }
                val sizeLabel = if (model.sizeInBytes > 0) "%.1f GB".format(model.sizeInBytes / 1_000_000_000.0) else "Unknown size"
                Text(
                    "$sizeLabel • ${status.status.name.replace("_", " ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForegroundMuted
                )
            }
            
            when (status.status) {
                NOT_DOWNLOADED, FAILED -> {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Rounded.CloudDownload, null, tint = AccentViolet)
                    }
                }
                IN_PROGRESS, PARTIALLY_DOWNLOADED, UNZIPPING -> {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Rounded.Cancel, null, tint = ForegroundMuted)
                    }
                }
                SUCCEEDED -> {
                    Row {
                        if (!isDefault) {
                            IconButton(onClick = onSetDefault) {
                                Icon(Icons.Rounded.CheckCircleOutline, null, tint = ForegroundMuted)
                            }
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Rounded.DeleteOutline, null, tint = AccentRed)
                        }
                    }
                }
            }
        }
        
        if (status.status == IN_PROGRESS || status.status == UNZIPPING) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = AccentViolet,
                    trackColor = SurfaceCard
                )
                if (status.bytesPerSecond > 0) {
                    Text(
                        "${status.bytesPerSecond / 1024} KB/s",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForegroundMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageActionItem(
    icon: ImageVector,
    label: String,
    color: Color = ForegroundPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                label = { Text(mode.name.lowercase().capitalize()) }
            )
        }
    }
}
