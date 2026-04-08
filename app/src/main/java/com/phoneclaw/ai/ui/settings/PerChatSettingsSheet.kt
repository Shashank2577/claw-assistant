package com.phoneclaw.ai.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneclaw.ai.data.db.entity.PerChatSettingsEntity
import com.phoneclaw.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerChatSettingsSheet(
    conversationId: String,
    onDismiss: () -> Unit,
    viewModel: PerChatSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(conversationId) {
        viewModel.loadSettings(conversationId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CanvasBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            HeaderSection(onDismiss)

            if (settings != null) {
                SettingsContent(
                    settings = settings!!,
                    onUpdate = viewModel::updateSettings
                )
            } else {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentViolet)
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Chat Settings",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = NunitoFontFamily
            ),
            color = ForegroundPrimary
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, null, tint = ForegroundSecondary)
        }
    }
}

@Composable
private fun SettingsContent(
    settings: PerChatSettingsEntity,
    onUpdate: (PerChatSettingsEntity) -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Temperature
        SliderSetting(
            label = "Temperature",
            value = settings.temperature,
            range = 0f..1f,
            onValueChange = { onUpdate(settings.copy(temperature = it)) }
        )

        // Top K
        SliderSetting(
            label = "Top K",
            value = settings.topK.toFloat(),
            range = 1f..100f,
            steps = 100,
            onValueChange = { onUpdate(settings.copy(topK = it.toInt())) }
        )

        // Top P
        SliderSetting(
            label = "Top P",
            value = settings.topP,
            range = 0f..1f,
            onValueChange = { onUpdate(settings.copy(topP = it)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onUpdate(PerChatSettingsEntity(conversationId = settings.conversationId)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SurfacePressed),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp), tint = ForegroundPrimary)
            Spacer(Modifier.width(8.dp))
            Text("Reset to Defaults", color = ForegroundPrimary)
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                text = if (steps > 0) value.toInt().toString() else "%.2f".format(value),
                style = MaterialTheme.typography.bodyMedium,
                color = AccentViolet,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = AccentViolet,
                activeTrackColor = AccentViolet,
                inactiveTrackColor = SurfacePressed
            )
        )
    }
}
