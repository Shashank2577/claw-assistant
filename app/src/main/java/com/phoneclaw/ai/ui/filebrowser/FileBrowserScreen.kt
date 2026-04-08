package com.phoneclaw.ai.ui.filebrowser

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phoneclaw.ai.ui.common.EmptyState

private val PurplePrimary = Color(0xFF7C3AED)
private val LavenderBackground = Color(0xFFF8F5FF)

private val FilterChip_SelectedBg = Color(0xFF7C3AED)
private val FilterChip_SelectedText = Color.White
private val FilterChip_UnselectedBorder = Color(0xFFD1D5DB)
private val FilterChip_UnselectedText = Color(0xFF6B7280)

private val StorageBarTrack = Color(0xFFE5E7EB)

private enum class FileFilter(val label: String) {
    ALL("All"),
    DOCS("Docs"),
    IMAGES("Images"),
    CODE("Code"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    spaceId: String,
    spaceName: String,
    onBack: () -> Unit,
    viewModel: FileBrowserViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(FileFilter.ALL) }

    LaunchedEffect(spaceId) {
        viewModel.loadFiles(spaceId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addFiles(uris)
        }
    }

    Scaffold(
        containerColor = LavenderBackground,
        topBar = {
            Column(modifier = Modifier.background(LavenderBackground)) {
                // Space indicator chip above title
                if (!isSearchActive) {
                    Row(
                        modifier = Modifier.padding(start = 56.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PurplePrimary.copy(alpha = 0.1f),
                        ) {
                            Text(
                                text = spaceName,
                                style = MaterialTheme.typography.labelSmall,
                                color = PurplePrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Back",
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    },
                    title = {
                        if (isSearchActive) {
                            SearchBar(
                                query = uiState.searchQuery,
                                onQueryChange = viewModel::setSearchQuery,
                                onClose = {
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                },
                            )
                        } else {
                            Text(
                                text = "Files",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Search files",
                                )
                            }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Sort,
                                        contentDescription = "Sort",
                                    )
                                }
                                SortDropdownMenu(
                                    expanded = showSortMenu,
                                    currentSort = uiState.sortBy,
                                    onDismiss = { showSortMenu = false },
                                    onSortSelected = {
                                        viewModel.setSortBy(it)
                                        showSortMenu = false
                                    },
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LavenderBackground,
                    ),
                )
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                // Filter chips row
                if (!isSearchActive) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(FileFilter.entries) { filter ->
                            FilterChipItem(
                                label = filter.label,
                                selected = activeFilter == filter,
                                onClick = { activeFilter = filter },
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                containerColor = PurplePrimary,
                contentColor = Color.White,
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Files",
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val searchFiltered = if (uiState.searchQuery.isBlank()) {
            uiState.files
        } else {
            uiState.files.filter { it.name.contains(uiState.searchQuery, ignoreCase = true) }
        }

        val displayFiles = when (activeFilter) {
            FileFilter.ALL -> searchFiltered
            FileFilter.DOCS -> searchFiltered.filter {
                it.mimeType.startsWith("application/") && !it.mimeType.startsWith("application/json")
            }
            FileFilter.IMAGES -> searchFiltered.filter { it.mimeType.startsWith("image/") }
            FileFilter.CODE -> searchFiltered.filter {
                it.mimeType.startsWith("text/") ||
                    it.mimeType.startsWith("application/json") ||
                    it.mimeType.startsWith("application/xml") ||
                    it.mimeType == "application/x-python" ||
                    it.mimeType == "text/x-python"
            }
        }

        if (!uiState.isLoading && displayFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Outlined.Folder,
                    title = "No files in this space",
                    subtitle = "Files you share in conversations appear here",
                    actionLabel = "Add Files",
                    onAction = { filePicker.launch(arrayOf("*/*")) },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Storage bar
                item {
                    StorageBar(usedGb = 2.4f, totalGb = 15.0f)
                }

                // Recent Files header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Recent Files",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        TextButton(onClick = { /* see all */ }) {
                            Text(
                                text = "See All →",
                                style = MaterialTheme.typography.labelMedium,
                                color = PurplePrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // File grid (2 columns) implemented as chunked rows inside LazyColumn
                val chunked = displayFiles.chunked(2)
                items(chunked) { rowFiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowFiles.forEach { file ->
                            FileGridCard(
                                file = file,
                                onClick = {},
                                onDeleteClick = { viewModel.showDeleteConfirm(file.path) },
                                onRenameClick = { viewModel.showRenameDialog(file.path) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Fill empty slot if odd number
                        if (rowFiles.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    uiState.showDeleteConfirmFor?.let { path ->
        val fileName = uiState.files.find { it.path == path }?.name ?: "this file"
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text("Delete file?") },
            text = { Text("\"$fileName\" will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteFile(path) },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirm) {
                    Text("Cancel")
                }
            },
        )
    }

    uiState.showRenameDialogFor?.let { path ->
        val original = uiState.files.find { it.path == path }?.name ?: ""
        RenameDialog(
            originalName = original,
            onConfirm = { newName -> viewModel.renameFile(path, newName) },
            onDismiss = viewModel::dismissRenameDialog,
        )
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (selected) FilterChip_SelectedBg else Color.Transparent
    val textColor = if (selected) FilterChip_SelectedText else FilterChip_UnselectedText
    val borderModifier = if (selected) {
        Modifier
    } else {
        Modifier.border(1.dp, FilterChip_UnselectedBorder, RoundedCornerShape(50))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .then(borderModifier)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun StorageBar(
    usedGb: Float,
    totalGb: Float,
    modifier: Modifier = Modifier,
) {
    val freeGb = totalGb - usedGb
    val fraction = (usedGb / totalGb).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Storage: ${usedGb} GB used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${"%.1f".format(freeGb)} GB free",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = PurplePrimary,
            trackColor = StorageBarTrack,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridCard(
    file: FileItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val iconColor = getFileTypeColor(file.mimeType)
    val icon = getFileTypeIcon(file.mimeType)

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showContextMenu = true },
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = iconColor,
                    )
                }
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (file.formattedSize.isNotEmpty()) {
                        Text(
                            text = file.formattedSize,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "\u00b7",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = file.formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            FileContextMenu(
                expanded = showContextMenu,
                onDismiss = { showContextMenu = false },
                onDelete = {
                    showContextMenu = false
                    onDeleteClick()
                },
                onRename = {
                    showContextMenu = false
                    onRenameClick()
                },
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search files...") },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = {
                focusManager.clearFocus()
                onClose()
            }) {
                Icon(Icons.Rounded.Close, contentDescription = "Close search")
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
    )
}

@Composable
private fun SortDropdownMenu(
    expanded: Boolean,
    currentSort: SortBy,
    onDismiss: () -> Unit,
    onSortSelected: (SortBy) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        SortBy.entries.forEach { sort ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = sort.label,
                        fontWeight = if (sort == currentSort) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                onClick = { onSortSelected(sort) },
            )
        }
    }
}

private val SortBy.label: String
    get() = when (this) {
        SortBy.NAME -> "Name"
        SortBy.DATE -> "Date modified"
        SortBy.SIZE -> "Size"
        SortBy.TYPE -> "File type"
    }

@Composable
private fun FileContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text("Rename") },
            onClick = onRename,
        )
        DropdownMenuItem(
            text = {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            },
            onClick = onDelete,
        )
    }
}

@Composable
private fun RenameDialog(
    originalName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by rememberSaveable { mutableStateOf(originalName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename file") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                label = { Text("File name") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank() && newName != originalName,
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
