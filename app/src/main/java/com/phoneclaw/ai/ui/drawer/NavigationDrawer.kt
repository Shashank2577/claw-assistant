package com.phoneclaw.ai.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val PurpleGradientStart = Color(0xFF7C3AED)
private val PurpleGradientEnd = Color(0xFFA855F7)
private val ActiveConversationTint = Color(0xFFF3EEFF)
private val SectionLabelGray = Color(0xFF9CA3AF)

@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    viewModel: DrawerViewModel,
    onConversationSelected: (conversationId: String) -> Unit,
    onNewChat: (conversationId: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToFiles: () -> Unit,
    content: @Composable () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversations = uiState.conversations
    val spaces = uiState.spaces
    val currentSpaceId = uiState.currentSpaceId
    val scope = rememberCoroutineScope()

    val currentSpace = spaces.firstOrNull { it.id == currentSpaceId }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFFCFAFF),
            ) {
                // Scrollable body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Header
                    DrawerHeader(
                        spaceEmoji = currentSpace?.icon ?: "\uD83D\uDCC1",
                        spaceName = currentSpace?.name ?: "Personal",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                    )

                    // New Chat button
                    Surface(
                        onClick = {
                            viewModel.createNewChat()
                            scope.launch { drawerState.close() }
                            onNewChat("")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(PurpleGradientStart, PurpleGradientEnd),
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "New Chat",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Conversations list (not lazy — inside scroll column)
                    ConversationList(
                        conversations = conversations,
                        currentConversationId = null,
                        onSelect = { id ->
                            scope.launch { drawerState.close() }
                            onConversationSelected(id)
                        },
                        onDelete = { id -> viewModel.deleteConversation(id) },
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Spacer(modifier = Modifier.height(4.dp))

                    // Spaces switcher
                    SpaceSwitcher(
                        spaces = spaces,
                        selectedSpaceId = currentSpaceId,
                        onSpaceSelect = { id ->
                            viewModel.switchSpace(id)
                            scope.launch { drawerState.close() }
                        },
                        onCreateSpace = {
                            viewModel.createSpace(name = "New Space")
                        },
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Footer — always visible at the bottom
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "Settings",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "Voice Mode",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToVoice()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "Files",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToFiles()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        content = content,
    )
}

@Composable
private fun DrawerHeader(
    spaceEmoji: String,
    spaceName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Large purple gradient circle avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PurpleGradientStart, PurpleGradientEnd),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "PocketAI",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$spaceEmoji $spaceName >",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
