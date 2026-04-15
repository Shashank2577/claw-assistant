package com.phoneclaw.ai.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.phoneclaw.ai.customtasks.agentchat.SkillManagerViewModel
import com.phoneclaw.ai.customtasks.agentchat.SkillState
import com.phoneclaw.ai.customtasks.agentchat.getSkillSecretKey
import com.phoneclaw.ai.ui.theme.AccentRed
import com.phoneclaw.ai.ui.theme.AccentViolet
import com.phoneclaw.ai.ui.theme.ForegroundPrimary
import com.phoneclaw.ai.ui.theme.ForegroundSecondary
import com.phoneclaw.ai.ui.theme.SurfaceCard
import com.phoneclaw.ai.ui.theme.SurfaceVioletCard
import com.phoneclaw.ai.ui.theme.customColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BUTTON_CONTENT_PADDING = PaddingValues(start = 12.dp, top = 2.dp, end = 12.dp, bottom = 2.dp)

private enum class AddSkillOptionType {
    RemoteUrl,
    LocalImport,
}

private data class AddSkillOption(
    val type: AddSkillOptionType,
    val title: String,
    val description: String,
    val icon: ImageVector,
)

private val ADD_SKILL_OPTIONS = listOf(
    AddSkillOption(
        type = AddSkillOptionType.RemoteUrl,
        title = "From URL",
        description = "Import a skill from a remote URL (SKILL.md file)",
        icon = Icons.Rounded.Link,
    ),
    AddSkillOption(
        type = AddSkillOptionType.LocalImport,
        title = "From Local File",
        description = "Import a skill from a local SKILL.md file",
        icon = Icons.Outlined.FileOpen,
    ),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SkillManagerBottomSheet(
    skillManagerViewModel: SkillManagerViewModel,
    onDismiss: (selectedSkillsChanged: Boolean) -> Unit,
) {
    val uiState by skillManagerViewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var showAddSkillFromUrlDialog by remember { mutableStateOf(false) }
    var showAddSkillFromLocalImportDialog by remember { mutableStateOf(false) }
    var showAddSkillOptionsSheet by remember { mutableStateOf(false) }
    var showDeleteSkillDialog by remember { mutableStateOf(false) }
    var showViewSkillDialog by remember { mutableStateOf(false) }
    var showSecretEditorDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }

    var skillToDeleteName by remember { mutableStateOf("") }
    var skillToView by remember { mutableStateOf<SkillState?>(null) }
    var skillToEditIndex by remember { mutableStateOf(-1) }
    var addSkillOptionTypeToConfirm by remember { mutableStateOf<AddSkillOptionType?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var savedSelectedSkillsNamesAndDescriptions = remember { "" }
    var filteredSkills by remember { mutableStateOf(uiState.skills) }
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    var hasDeterminedExpansionStates by remember { mutableStateOf(false) }
    var isBuiltInExpanded by remember { mutableStateOf(false) }
    var isCustomExpanded by remember { mutableStateOf(true) }
    var inMultiSelectMode by remember { mutableStateOf(false) }
    val selectedCustomSkillNames = remember { mutableStateListOf<String>() }
    var previousSearchQuery by remember { mutableStateOf(searchQuery) }

    var showSkillLimitBanner by remember { mutableStateOf(false) }
    val selectedSkillsCount by remember {
        derivedStateOf { uiState.skills.count { it.skill.selected } }
    }

    LaunchedEffect(selectedSkillsCount) {
        if (selectedSkillsCount > 5) {
            showSkillLimitBanner = true
        }
    }

    LaunchedEffect(showSkillLimitBanner) {
        if (showSkillLimitBanner) {
            delay(3000)
            showSkillLimitBanner = false
        }
    }

    LaunchedEffect(uiState.skills, searchQuery, uiState.loading) {
        if (!uiState.loading && !hasDeterminedExpansionStates) {
            val hasCustomSkills = uiState.skills.any { !it.skill.builtIn }
            isBuiltInExpanded = !hasCustomSkills
            hasDeterminedExpansionStates = true
        }

        val trimmedQuery = searchQuery.trim().lowercase()
        filteredSkills =
            if (trimmedQuery.isBlank()) {
                uiState.skills
            } else {
                uiState.skills.filter { skillState ->
                    val skill = skillState.skill
                    skill.name.lowercase().contains(trimmedQuery) ||
                        skill.description.lowercase().contains(trimmedQuery)
                }
            }

        if (searchQuery != previousSearchQuery) {
            if (searchQuery.isNotEmpty()) {
                isBuiltInExpanded = true
                isCustomExpanded = true
            }
            if (filteredSkills.isNotEmpty()) {
                listState.scrollToItem(0)
            }
            previousSearchQuery = searchQuery
        }
    }

    LaunchedEffect(Unit) {
        savedSelectedSkillsNamesAndDescriptions =
            skillManagerViewModel.getSelectedSkillsNamesAndDescriptions()
    }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss(
                savedSelectedSkillsNamesAndDescriptions !=
                    skillManagerViewModel.getSelectedSkillsNamesAndDescriptions()
            )
        },
        sheetState = sheetState,
        containerColor = SurfaceCard,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Spinner when loading.
            if (uiState.loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 3.dp,
                        modifier = Modifier.padding(end = 8.dp).size(24.dp),
                        color = AccentViolet,
                    )
                }
            } else {
                val focusManager = LocalFocusManager.current

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                ) {
                    // Title or Multi-Select Context Bar.
                    if (inMultiSelectMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = {
                                    inMultiSelectMode = false
                                    selectedCustomSkillNames.clear()
                                }
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
                            Text(
                                "${selectedCustomSkillNames.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                color = ForegroundPrimary,
                            )
                            IconButton(
                                modifier = Modifier.padding(end = 3.dp),
                                onClick = {
                                    if (selectedCustomSkillNames.isNotEmpty()) {
                                        showDeleteSkillDialog = true
                                    }
                                },
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Manage Skills",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ForegroundPrimary,
                                )
                                Text(
                                    "Enable or disable skills for the AI agent",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ForegroundSecondary,
                                )
                            }
                            IconButton(
                                modifier = Modifier.padding(end = 3.dp),
                                onClick = {
                                    scope.launch {
                                        sheetState.hide()
                                        onDismiss(
                                            savedSelectedSkillsNamesAndDescriptions !=
                                                skillManagerViewModel.getSelectedSkillsNamesAndDescriptions()
                                        )
                                    }
                                },
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = if (searchQuery.isEmpty()) 8.dp else 18.dp)
                            .height(IntrinsicSize.Min),
                    ) {
                        // Search bar.
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            placeholder = { Text("Search skills") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.trim().isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Outlined.Cancel, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedContainerColor = SurfaceVioletCard,
                                unfocusedContainerColor = SurfaceVioletCard,
                            ),
                        )

                        // Button to add skill.
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .clickable {
                                    searchQuery = ""
                                    showAddSkillOptionsSheet = true
                                }
                                .background(AccentViolet),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Add skill",
                                tint = Color.White,
                            )
                        }
                    }

                    AnimatedVisibility(visible = searchQuery.isEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        ) {
                            Text(
                                "${uiState.skills.size} skill${if (uiState.skills.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelLarge,
                                color = ForegroundSecondary,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { skillManagerViewModel.setAllSkillsSelected(selected = true) }
                                ) {
                                    Text("Enable All", color = AccentViolet)
                                }
                                TextButton(
                                    onClick = { skillManagerViewModel.setAllSkillsSelected(selected = false) }
                                ) {
                                    Text("Disable All", color = AccentViolet)
                                }
                            }
                        }
                    }

                    // Content — disable over-scroll stretch.
                    CompositionLocalProvider(LocalOverscrollFactory provides null) {
                        val builtInSkills = remember(filteredSkills) { filteredSkills.filter { it.skill.builtIn } }
                        val customSkills = remember(filteredSkills) { filteredSkills.filter { !it.skill.builtIn } }

                        Box(modifier = Modifier.weight(1f)) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (builtInSkills.isNotEmpty()) {
                                    item(key = "built_in_header") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(shape = RoundedCornerShape(20.dp))
                                                .clickable { isBuiltInExpanded = !isBuiltInExpanded }
                                                .padding(vertical = 12.dp, horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                "Built-in",
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.weight(1f),
                                                color = ForegroundPrimary,
                                            )
                                            Icon(
                                                imageVector = if (isBuiltInExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                contentDescription = if (isBuiltInExpanded) "Collapse" else "Expand",
                                            )
                                        }
                                    }
                                    if (isBuiltInExpanded) {
                                        items(builtInSkills, key = { it.skill.name }) { skillState ->
                                            SkillItemRow(
                                                skillState = skillState,
                                                inMultiSelectMode = inMultiSelectMode,
                                                isSelectedForDeletion = false,
                                                onSelectionCheckedChange = {},
                                                onLongClick = {},
                                                onSkillEnabledChange = { newChecked ->
                                                    skillManagerViewModel.setSkillSelected(skillState, newChecked)
                                                },
                                                onViewClick = {
                                                    skillToView = skillState
                                                    showViewSkillDialog = true
                                                },
                                                onSecretClick = {
                                                    skillToEditIndex = uiState.skills.indexOf(skillState)
                                                    showSecretEditorDialog = true
                                                },
                                                onDeleteClick = {
                                                    skillToDeleteName = skillState.skill.name
                                                    showDeleteSkillDialog = true
                                                },
                                                uriHandler = uriHandler,
                                            )
                                        }
                                    }
                                }

                                if (customSkills.isNotEmpty()) {
                                    item(key = "custom_header") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(shape = RoundedCornerShape(20.dp))
                                                .clickable { isCustomExpanded = !isCustomExpanded }
                                                .padding(vertical = 12.dp, horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                "Custom",
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.weight(1f),
                                                color = ForegroundPrimary,
                                            )
                                            Icon(
                                                imageVector = if (isCustomExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                contentDescription = if (isCustomExpanded) "Collapse" else "Expand",
                                            )
                                        }
                                    }
                                    if (isCustomExpanded) {
                                        items(customSkills, key = { it.skill.name }) { skillState ->
                                            SkillItemRow(
                                                skillState = skillState,
                                                inMultiSelectMode = inMultiSelectMode,
                                                isSelectedForDeletion = selectedCustomSkillNames.contains(skillState.skill.name),
                                                onSelectionCheckedChange = { checked ->
                                                    if (checked) {
                                                        selectedCustomSkillNames.add(skillState.skill.name)
                                                    } else {
                                                        selectedCustomSkillNames.remove(skillState.skill.name)
                                                        if (selectedCustomSkillNames.isEmpty()) inMultiSelectMode = false
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!inMultiSelectMode) {
                                                        inMultiSelectMode = true
                                                        selectedCustomSkillNames.add(skillState.skill.name)
                                                    }
                                                },
                                                onSkillEnabledChange = { newChecked ->
                                                    skillManagerViewModel.setSkillSelected(skillState, newChecked)
                                                },
                                                onViewClick = {
                                                    skillToView = skillState
                                                    showViewSkillDialog = true
                                                },
                                                onSecretClick = {
                                                    skillToEditIndex = uiState.skills.indexOf(skillState)
                                                    showSecretEditorDialog = true
                                                },
                                                onDeleteClick = {
                                                    skillToDeleteName = skillState.skill.name
                                                    showDeleteSkillDialog = true
                                                },
                                                uriHandler = uriHandler,
                                            )
                                        }
                                    }
                                }
                            }

                            // Skill limit banner.
                            if (showSkillLimitBanner) {
                                Text(
                                    "More than 5 skills selected. Consider disabling some for best performance.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentViolet)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog.
    if (showDeleteSkillDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSkillDialog = false },
            title = {
                Text(
                    if (inMultiSelectMode) "Delete Selected Skills" else "Delete Skill"
                )
            },
            text = {
                Text(
                    if (inMultiSelectMode)
                        "Delete ${selectedCustomSkillNames.size} selected skill${if (selectedCustomSkillNames.size != 1) "s" else ""}? This cannot be undone."
                    else
                        "Delete this skill? This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inMultiSelectMode) {
                            skillManagerViewModel.deleteSkills(selectedCustomSkillNames.toSet())
                            inMultiSelectMode = false
                            selectedCustomSkillNames.clear()
                        } else {
                            skillManagerViewModel.deleteSkill(name = skillToDeleteName)
                        }
                        showDeleteSkillDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteSkillDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // View skill dialog.
    if (showViewSkillDialog) {
        skillToView?.let { state ->
            AlertDialog(
                onDismissRequest = { showViewSkillDialog = false },
                title = { Text(state.skill.name) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            state.skill.instructions,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showViewSkillDialog = false }) {
                        Text("Close")
                    }
                },
            )
        }
    }

    // Add skill from URL dialog.
    if (showAddSkillFromUrlDialog) {
        AddSkillFromUrlDialog(
            skillManagerViewModel = skillManagerViewModel,
            onDismissRequest = { showAddSkillFromUrlDialog = false },
            onSuccess = { scrollToBottomOfList(scope, listState) },
        )
    }

    // Add skill from local file dialog.
    if (showAddSkillFromLocalImportDialog) {
        AddSkillFromLocalImportDialog(
            skillManagerViewModel = skillManagerViewModel,
            onDismissRequest = { showAddSkillFromLocalImportDialog = false },
            onSuccess = { scrollToBottomOfList(scope, listState) },
        )
    }

    // Add skill options bottom sheet.
    if (showAddSkillOptionsSheet) {
        AddSkillOptionsBottomSheet(
            onDismiss = { showAddSkillOptionsSheet = false },
            onOptionSelected = { option ->
                skillManagerViewModel.setValidationError(null)
                addSkillOptionTypeToConfirm = option.type
                when (option.type) {
                    AddSkillOptionType.RemoteUrl -> {
                        showAddSkillFromUrlDialog = true
                    }
                    AddSkillOptionType.LocalImport -> {
                        showDisclaimerDialog = true
                    }
                }
                showAddSkillOptionsSheet = false
            },
        )
    }

    // Secret editor dialog.
    if (showSecretEditorDialog) {
        val skillState = uiState.skills.getOrNull(skillToEditIndex)
        skillState?.let { state: SkillState ->
            var curSecret by remember {
                mutableStateOf(
                    skillManagerViewModel.dataStoreRepository.readSecret(
                        getSkillSecretKey(skillName = state.skill.name)
                    ) ?: ""
                )
            }
            SecretEditorDialog(
                title = "Edit Secret",
                fieldLabel = state.skill.requireSecretDescription,
                value = curSecret,
                onValueChange = { curSecret = it },
                onDone = {
                    skillManagerViewModel.dataStoreRepository.saveSecret(
                        key = getSkillSecretKey(skillName = state.skill.name),
                        value = curSecret,
                    )
                    showSecretEditorDialog = false
                },
                onDismiss = { showSecretEditorDialog = false },
            )
        }
    }

    // Disclaimer dialog.
    if (showDisclaimerDialog) {
        AddSkillDisclaimerDialog(
            onDismiss = {
                showDisclaimerDialog = false
                addSkillOptionTypeToConfirm = null
            },
            onConfirm = {
                addSkillOptionTypeToConfirm?.let { type ->
                    if (type == AddSkillOptionType.LocalImport) {
                        showAddSkillFromLocalImportDialog = true
                    }
                }
                showDisclaimerDialog = false
                addSkillOptionTypeToConfirm = null
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkillItemRow(
    skillState: SkillState,
    inMultiSelectMode: Boolean,
    isSelectedForDeletion: Boolean,
    onSelectionCheckedChange: (Boolean) -> Unit,
    onLongClick: () -> Unit,
    onSkillEnabledChange: (Boolean) -> Unit,
    onViewClick: () -> Unit,
    onSecretClick: () -> Unit,
    onDeleteClick: () -> Unit,
    uriHandler: androidx.compose.ui.platform.UriHandler,
) {
    val skill = skillState.skill
    val isCustom = !skill.builtIn

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (inMultiSelectMode && skill.builtIn) Modifier.alpha(0.5f) else Modifier)
            .clip(shape = RoundedCornerShape(20.dp))
            .background(SurfaceVioletCard)
            .then(
                if (isCustom) {
                    Modifier.combinedClickable(
                        onClick = {
                            if (inMultiSelectMode) {
                                onSelectionCheckedChange(!isSelectedForDeletion)
                            }
                        },
                        onLongClick = onLongClick,
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (inMultiSelectMode && isCustom) {
            Checkbox(
                checked = isSelectedForDeletion,
                onCheckedChange = onSelectionCheckedChange,
                modifier = Modifier.padding(end = 12.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    modifier = Modifier.weight(1f).padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val hasHomepage = skill.homepage.isNotBlank()
                        val textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)

                        if (hasHomepage) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    skill.name,
                                    style = textStyle.copy(
                                        color = AccentViolet,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                    color = MaterialTheme.customColors.linkColor,
                                    modifier = Modifier.clickable { uriHandler.openUri(skill.homepage) },
                                )
                                Icon(
                                    Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.customColors.linkColor,
                                )
                            }
                        } else {
                            Text(skill.name, style = textStyle, color = ForegroundPrimary)
                        }
                    }
                    Text(
                        skill.description.replace("\n", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = ForegroundSecondary,
                    )
                }

                Switch(
                    checked = skill.selected,
                    onCheckedChange = onSkillEnabledChange,
                    modifier = Modifier.offset(y = (-4).dp),
                    enabled = !inMultiSelectMode,
                )
            }

            AnimatedVisibility(visible = !inMultiSelectMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    FilledTonalButton(
                        onClick = onViewClick,
                        modifier = Modifier.height(32.dp).padding(end = 8.dp),
                        contentPadding = BUTTON_CONTENT_PADDING,
                    ) {
                        Icon(
                            Icons.Outlined.RemoveRedEye,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "View",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    if (skill.requireSecret) {
                        FilledTonalButton(
                            onClick = onSecretClick,
                            modifier = Modifier.height(32.dp).padding(end = 8.dp),
                            contentPadding = BUTTON_CONTENT_PADDING,
                        ) {
                            Icon(
                                Icons.Outlined.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "Secret",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }

                    if (!skill.builtIn) {
                        OutlinedButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.height(32.dp),
                            contentPadding = BUTTON_CONTENT_PADDING,
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "Delete",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun scrollToBottomOfList(scope: CoroutineScope, listState: LazyListState) {
    scope.launch {
        delay(300)
        if (listState.layoutInfo.totalItemsCount > 0) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSkillOptionsBottomSheet(
    onDismiss: () -> Unit,
    onOptionSelected: (AddSkillOption) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                "Add Skill",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp).padding(horizontal = 16.dp),
                color = ForegroundPrimary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ADD_SKILL_OPTIONS.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 4.dp),
                            ) {
                                Icon(option.icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = AccentViolet)
                                Text(option.title, style = MaterialTheme.typography.bodyLarge, color = ForegroundPrimary)
                            }
                            Text(
                                option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = ForegroundSecondary,
                                modifier = Modifier.padding(start = 40.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSkillFromUrlDialog(
    skillManagerViewModel: SkillManagerViewModel,
    onDismissRequest: () -> Unit,
    onSuccess: () -> Unit,
) {
    val uiState by skillManagerViewModel.uiState.collectAsState()
    val validating = uiState.validating
    val validationError = uiState.validationError

    var urlText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { if (!validating) onDismissRequest() }) {
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Add Skill from URL",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Enter SKILL.md URL", style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { newVal ->
                            urlText = newVal
                            skillManagerViewModel.setValidationError(null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://example.com/SKILL.md") },
                        singleLine = false,
                        maxLines = 3,
                    )
                    validationError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (validating) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AccentViolet,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        OutlinedButton(onClick = { onDismissRequest() }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = {
                                skillManagerViewModel.importSkillFromUrl(urlText) { skill, error ->
                                    if (skill != null) {
                                        onDismissRequest()
                                        onSuccess()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSkillFromLocalImportDialog(
    skillManagerViewModel: SkillManagerViewModel,
    onDismissRequest: () -> Unit,
    onSuccess: () -> Unit,
) {
    val uiState by skillManagerViewModel.uiState.collectAsState()
    val validating = uiState.validating
    val validationError = uiState.validationError

    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        selectedUri = uri
        skillManagerViewModel.setValidationError(null)
    }

    Dialog(onDismissRequest = onDismissRequest) {
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        "Add Skill from Local File",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Select a SKILL.md file from your device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Select file", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = selectedUri?.lastPathSegment ?: "No file selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedUri == null)
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Outlined.FileOpen, contentDescription = "Pick file", tint = AccentViolet)
                        }
                    }
                    validationError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (validating) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AccentViolet,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        OutlinedButton(onClick = onDismissRequest) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            enabled = selectedUri != null,
                            onClick = {
                                selectedUri?.let { uri ->
                                    skillManagerViewModel.importSkillFromLocalUri(uri) { skill, error ->
                                        if (skill != null) {
                                            onDismissRequest()
                                            onSuccess()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSkillDisclaimerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Security Notice") },
        text = {
            Text(
                "You are about to import a skill from an external source. " +
                    "Custom skills can execute code and access device features. " +
                    "Only import skills from sources you trust."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
            ) {
                Text("I Understand")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
