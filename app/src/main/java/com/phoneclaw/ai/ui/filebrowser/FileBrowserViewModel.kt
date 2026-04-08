package com.phoneclaw.ai.ui.filebrowser

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoneclaw.ai.data.repository.SpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// File type colors per design spec
val FileTypePdfColor = Color(0xFFEF4444)       // red/pink — PDF
val FileTypeImageColor = Color(0xFF14B8A6)     // teal — images
val FileTypeCodeColor = Color(0xFF3B82F6)      // blue — code/text
val FileTypeDataColor = Color(0xFFF97316)      // orange — CSV/spreadsheets
val FileTypePresentationColor = Color(0xFF8B5CF6) // purple — presentations
val FileTypeAudioColor = Color(0xFFEC4899)     // pink — audio
val FileTypeVideoColor = Color(0xFF6366F1)     // indigo — video
val FileTypeDefaultColor = Color(0xFF6B7280)   // gray — generic

fun getFileTypeColor(mimeType: String): Color = when {
    mimeType == "application/pdf" -> FileTypePdfColor
    mimeType.startsWith("image/") -> FileTypeImageColor
    mimeType.startsWith("audio/") -> FileTypeAudioColor
    mimeType.startsWith("video/") -> FileTypeVideoColor
    mimeType == "text/csv" ||
        mimeType == "application/vnd.ms-excel" ||
        mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> FileTypeDataColor
    mimeType == "application/vnd.ms-powerpoint" ||
        mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" ||
        mimeType == "application/vnd.apple.keynote" -> FileTypePresentationColor
    mimeType.startsWith("text/") ||
        mimeType.startsWith("application/json") ||
        mimeType.startsWith("application/xml") ||
        mimeType == "application/x-python" ||
        mimeType == "text/x-python" -> FileTypeCodeColor
    else -> FileTypeDefaultColor
}

fun getFileTypeIcon(mimeType: String): ImageVector = when {
    mimeType == "application/pdf" -> Icons.Outlined.PictureAsPdf
    mimeType.startsWith("image/") -> Icons.Outlined.Image
    mimeType.startsWith("audio/") -> Icons.Outlined.AudioFile
    mimeType.startsWith("video/") -> Icons.Outlined.VideoFile
    mimeType == "text/csv" ||
        mimeType == "application/vnd.ms-excel" ||
        mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> Icons.Outlined.TableChart
    mimeType == "application/vnd.ms-powerpoint" ||
        mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" ||
        mimeType == "application/vnd.apple.keynote" -> Icons.Outlined.Slideshow
    mimeType.startsWith("text/") ||
        mimeType.startsWith("application/json") ||
        mimeType.startsWith("application/xml") ||
        mimeType == "application/x-python" ||
        mimeType == "text/x-python" -> Icons.Outlined.Code
    mimeType.startsWith("application/") -> Icons.Outlined.Description
    else -> Icons.Outlined.InsertDriveFile
}

data class FileItem(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String,
    val isDirectory: Boolean,
) {
    val formattedSize: String
        get() = when {
            isDirectory -> ""
            sizeBytes < 1_024L -> "$sizeBytes B"
            sizeBytes < 1_024L * 1_024L -> "${"%.1f".format(sizeBytes / 1_024f)} KB"
            sizeBytes < 1_024L * 1_024L * 1_024L -> "${"%.1f".format(sizeBytes / (1_024f * 1_024f))} MB"
            else -> "${"%.1f".format(sizeBytes / (1_024f * 1_024f * 1_024f))} GB"
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }
}

enum class ViewMode { GRID, LIST }

enum class SortBy { NAME, DATE, SIZE, TYPE }

data class FileBrowserUiState(
    val files: List<FileItem> = emptyList(),
    val viewMode: ViewMode = ViewMode.LIST,
    val sortBy: SortBy = SortBy.DATE,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val showDeleteConfirmFor: String? = null,
    val showRenameDialogFor: String? = null,
)

@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spaceRepository: SpaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private var currentSpaceId: String = ""

    fun loadFiles(spaceId: String) {
        currentSpaceId = spaceId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val files = withContext(Dispatchers.IO) {
                scanSpaceDirectory(spaceId)
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                files = sortFiles(files, _uiState.value.sortBy),
            )
        }
    }

    private fun scanSpaceDirectory(spaceId: String): List<FileItem> {
        val dir = getSpaceFilesDir(spaceId)
        if (!dir.exists()) {
            dir.mkdirs()
            return emptyList()
        }
        return dir.listFiles()
            ?.map { file -> file.toFileItem() }
            ?: emptyList()
    }

    private fun getSpaceFilesDir(spaceId: String): File {
        val base = context.getExternalFilesDir(null)
            ?: context.filesDir
        return File(base, "spaces/$spaceId/files")
    }

    private fun File.toFileItem(): FileItem {
        val ext = extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
        return FileItem(
            name = name,
            path = absolutePath,
            sizeBytes = if (isDirectory) 0L else length(),
            lastModified = lastModified(),
            mimeType = mime,
            isDirectory = isDirectory,
        )
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                File(path).delete()
            }
            _uiState.value = _uiState.value.copy(
                files = _uiState.value.files.filter { it.path != path },
                showDeleteConfirmFor = null,
            )
        }
    }

    fun renameFile(path: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val renamed = withContext(Dispatchers.IO) {
                val file = File(path)
                val target = File(file.parentFile, newName)
                if (file.renameTo(target)) target else null
            }
            if (renamed != null) {
                _uiState.value = _uiState.value.copy(
                    files = _uiState.value.files.map { item ->
                        if (item.path == path) item.copy(name = newName, path = renamed.absolutePath)
                        else item
                    },
                    showRenameDialogFor = null,
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Could not rename file",
                    showRenameDialogFor = null,
                )
            }
        }
    }

    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val newItems = withContext(Dispatchers.IO) {
                val destDir = getSpaceFilesDir(currentSpaceId).also { it.mkdirs() }
                uris.mapNotNull { uri -> copyUriToDir(uri, destDir) }
            }
            val updated = sortFiles(
                _uiState.value.files + newItems,
                _uiState.value.sortBy,
            )
            _uiState.value = _uiState.value.copy(isLoading = false, files = updated)
        }
    }

    private fun copyUriToDir(uri: Uri, destDir: File): FileItem? {
        return runCatching {
            val fileName = resolveFileName(uri) ?: "file_${System.currentTimeMillis()}"
            val dest = uniqueFile(destDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.toFileItem()
        }.getOrNull()
    }

    private fun resolveFileName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx)
            }
        }
        return name ?: uri.lastPathSegment
    }

    private fun uniqueFile(dir: File, name: String): File {
        var candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val dotIdx = name.lastIndexOf('.')
        val base = if (dotIdx >= 0) name.substring(0, dotIdx) else name
        val ext = if (dotIdx >= 0) name.substring(dotIdx) else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base($counter)$ext")
            counter++
        }
        return candidate
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(
            viewMode = if (_uiState.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST,
        )
    }

    fun setSortBy(sort: SortBy) {
        _uiState.value = _uiState.value.copy(
            sortBy = sort,
            files = sortFiles(_uiState.value.files, sort),
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun showDeleteConfirm(path: String) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmFor = path)
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmFor = null)
    }

    fun showRenameDialog(path: String) {
        _uiState.value = _uiState.value.copy(showRenameDialogFor = path)
    }

    fun dismissRenameDialog() {
        _uiState.value = _uiState.value.copy(showRenameDialogFor = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun sortFiles(files: List<FileItem>, sort: SortBy): List<FileItem> {
        val dirs = files.filter { it.isDirectory }
        val regular = files.filter { !it.isDirectory }
        val sortedRegular = when (sort) {
            SortBy.NAME -> regular.sortedBy { it.name.lowercase() }
            SortBy.DATE -> regular.sortedByDescending { it.lastModified }
            SortBy.SIZE -> regular.sortedByDescending { it.sizeBytes }
            SortBy.TYPE -> regular.sortedWith(
                compareBy({ it.mimeType }, { it.name.lowercase() }),
            )
        }
        return dirs.sortedBy { it.name.lowercase() } + sortedRegular
    }

    val filteredFiles: StateFlow<List<FileItem>> = _uiState
        .map { state ->
            if (state.searchQuery.isBlank()) {
                state.files
            } else {
                state.files.filter { f ->
                    f.name.contains(state.searchQuery, ignoreCase = true)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}
