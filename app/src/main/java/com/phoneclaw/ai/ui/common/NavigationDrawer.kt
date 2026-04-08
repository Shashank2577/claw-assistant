package com.phoneclaw.ai.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneclaw.ai.ui.drawer.DrawerViewModel
import com.phoneclaw.ai.ui.drawer.SpaceSwitcher
import com.phoneclaw.ai.ui.theme.*

@Composable
fun AppNavigationDrawer(
    currentConversationId: String?,
    onConversationClick: (String) -> Unit,
    onNewConversation: () -> Unit,
    onSettingsClick: () -> Unit,
    onVoiceClick: () -> Unit = {},
    onFilesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DrawerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Row(modifier = modifier.fillMaxSize()) {
        // Space Switcher
        SpaceSwitcher(
            spaces = uiState.spaces,
            selectedSpaceId = uiState.currentSpaceId,
            onSpaceSelect = { viewModel.switchSpace(it) },
            onCreateSpace = { viewModel.createSpace("New Space") }
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .background(CanvasBg)
                .padding(bottom = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(AccentViolet, AccentVioletLight)),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = ForegroundInverse,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "PocketAI",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = NunitoFontFamily,
                        fontSize = 22.sp
                    ),
                    color = ForegroundPrimary
                )
            }

            // New Chat Button
            Surface(
                onClick = { 
                    viewModel.createNewChat()
                    onNewConversation()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(AccentViolet, AccentVioletLight)),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Add, null, tint = ForegroundInverse)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "New Chat",
                        color = ForegroundInverse,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Recent Chats Section
            Text(
                "RECENT",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = ForegroundMuted
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.conversations, key = { it.id }) { conversation ->
                    DrawerChatItem(
                        title = conversation.title,
                        isSelected = conversation.id == currentConversationId,
                        onClick = { onConversationClick(conversation.id) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp), color = BorderMuted)

            // Footer Links
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FooterIcon(Icons.Rounded.Settings, "Settings", onSettingsClick)
                FooterIcon(Icons.Rounded.Mic, "Voice", onVoiceClick)
                FooterIcon(Icons.Rounded.FolderOpen, "Files", onFilesClick)
            }
        }
    }
}

@Composable
private fun DrawerChatItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SurfacePressed else SurfaceCard,
        shadowElevation = if (isSelected) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.ChatBubbleOutline,
                null,
                tint = if (isSelected) AccentViolet else ForegroundSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) AccentViolet else ForegroundPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FooterIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Icon(icon, null, tint = ForegroundSecondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = ForegroundMuted)
    }
}
