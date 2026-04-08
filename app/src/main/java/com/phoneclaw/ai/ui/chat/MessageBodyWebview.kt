package com.phoneclaw.ai.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.phoneclaw.ai.data.model.ChatMessageData
import com.phoneclaw.ai.ui.common.OpenClawWebView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBodyWebview(message: ChatMessageData, modifier: Modifier = Modifier) {
    if (message.webviewUrl == null) return
    
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        OpenClawWebView(
            modifier = Modifier.fillMaxWidth().aspectRatio(message.aspectRatio),
            initialUrl = message.webviewUrl,
            useIframeWrapper = message.isIframe,
            preventParentScrolling = true,
            allowRequestPermission = true,
        )
        AssistChip(
            onClick = { showBottomSheet = true },
            leadingIcon = {
                Icon(
                    Icons.Outlined.FitScreen,
                    contentDescription = null,
                    Modifier.size(AssistChipDefaults.IconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            label = { Text("View in full screen") },
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                OpenClawWebView(
                    modifier = Modifier.fillMaxSize(),
                    initialUrl = message.webviewUrl,
                    useIframeWrapper = message.isIframe,
                    preventParentScrolling = true,
                    allowRequestPermission = true,
                )
                OutlinedIconButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            showBottomSheet = false
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
