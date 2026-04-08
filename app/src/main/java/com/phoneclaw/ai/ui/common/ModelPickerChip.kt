package com.phoneclaw.ai.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phoneclaw.ai.ui.theme.OpenClawAITheme

/**
 * Compact chip that displays the currently selected model name in the top bar.
 * Tapping it opens the model picker (handled by the caller via [onClick]).
 *
 * The chip uses [AssistChip] so it renders with a leading icon slot and a
 * trailing dropdown arrow, keeping the height touch-target-safe at ≥ 48 dp
 * when placed inside a [androidx.compose.material3.TopAppBar].
 *
 * @param modelName  Display name of the active model (e.g. "Gemma 4").
 * @param onClick    Called when the chip is tapped.
 * @param modifier   Layout modifier.
 * @param enabled    Whether the chip responds to taps (e.g. disable while
 *                   the model is loading).
 */
@Composable
fun ModelPickerChip(
    modelName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = modelName,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
        enabled = enabled,
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            trailingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = null,
    )
}

@Preview(showBackground = true, name = "ModelPickerChip - Enabled")
@Composable
private fun ModelPickerChipPreview() {
    OpenClawAITheme {
        ModelPickerChip(
            modelName = "Gemma 4",
            onClick = {},
        )
    }
}

@Preview(showBackground = true, name = "ModelPickerChip - Long Name")
@Composable
private fun ModelPickerChipLongNamePreview() {
    OpenClawAITheme {
        ModelPickerChip(
            modelName = "Gemma 3 27B Instruction Tuned",
            onClick = {},
        )
    }
}

@Preview(showBackground = true, name = "ModelPickerChip - Disabled")
@Composable
private fun ModelPickerChipDisabledPreview() {
    OpenClawAITheme {
        ModelPickerChip(
            modelName = "Gemma 4",
            onClick = {},
            enabled = false,
        )
    }
}
