package com.phoneclaw.ai.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phoneclaw.ai.ui.theme.OpenClawAITheme

private val GreenTest = Color(0xFF22C55E)

/**
 * API key input section used inside SettingsScreen.
 *
 * @param apiKey            Current raw key value (not masked by state — masking is visual).
 * @param isKeyVisible      Whether the key characters are currently shown in plain text.
 * @param onApiKeyChange    Called on every keystroke with the new value.
 * @param onToggleVisibility Called when the eye icon is tapped.
 * @param onTestConnection  Called when "Test" button is tapped.
 * @param onSave            Called when focus leaves the field to persist the key.
 * @param isValid           Tri-state: null = untested, true = valid, false = invalid.
 * @param isTesting         True while a test request is in-flight.
 */
@Composable
fun ApiKeySection(
    apiKey: String,
    isKeyVisible: Boolean,
    onApiKeyChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onTestConnection: () -> Unit,
    onSave: (String) -> Unit,
    isValid: Boolean?,
    isTesting: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("Gemini API Key") },
            placeholder = { Text("AIza…") },
            singleLine = true,
            visualTransformation = if (isKeyVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (isKeyVisible) {
                            Icons.Rounded.VisibilityOff
                        } else {
                            Icons.Rounded.Visibility
                        },
                        contentDescription = if (isKeyVisible) "Hide API key" else "Show API key",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    onSave(apiKey)
                    onTestConnection()
                },
                enabled = !isTesting && apiKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenTest,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(50),
            ) {
                Text("Test")
            }

            Spacer(modifier = Modifier.width(12.dp))

            AnimatedVisibility(
                visible = isTesting,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }

            AnimatedVisibility(
                visible = !isTesting && isValid == true,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "API key valid",
                    tint = Color(0xFF34A853),
                    modifier = Modifier.size(20.dp),
                )
            }

            AnimatedVisibility(
                visible = !isTesting && isValid == false,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "API key invalid",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Keys are encrypted and stored locally",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ApiKeySectionPreview() {
    OpenClawAITheme {
        ApiKeySection(
            apiKey = "AIzaSyExample",
            isKeyVisible = false,
            onApiKeyChange = {},
            onToggleVisibility = {},
            onTestConnection = {},
            onSave = {},
            isValid = null,
            isTesting = false,
        )
    }
}

@Preview(showBackground = true, name = "Valid key")
@Composable
private fun ApiKeySectionValidPreview() {
    OpenClawAITheme {
        ApiKeySection(
            apiKey = "AIzaSyExample",
            isKeyVisible = false,
            onApiKeyChange = {},
            onToggleVisibility = {},
            onTestConnection = {},
            onSave = {},
            isValid = true,
            isTesting = false,
        )
    }
}

@Preview(showBackground = true, name = "Invalid key")
@Composable
private fun ApiKeySectionInvalidPreview() {
    OpenClawAITheme {
        ApiKeySection(
            apiKey = "bad-key",
            isKeyVisible = false,
            onApiKeyChange = {},
            onToggleVisibility = {},
            onTestConnection = {},
            onSave = {},
            isValid = false,
            isTesting = false,
        )
    }
}
