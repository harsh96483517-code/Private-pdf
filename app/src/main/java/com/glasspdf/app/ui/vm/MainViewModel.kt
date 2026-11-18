package com.glasspdf.app.ui.vm

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glasspdf.app.GlassPdfApp
import com.glasspdf.app.data.AppSettings
import com.glasspdf.app.data.CoverStyle
import com.glasspdf.app.data.DateFilter
import com.glasspdf.app.data.FilterMode
import com.glasspdf.app.data.FolderEntity
import com.glasspdf.app.data.LayoutMode
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.data.SortField
import com.glasspdf.app.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as GlassPdfApp).container
    val repo = container.repository
    val thumbs = container.thumbnails
    private val store = container.settingsStore
    private val context = app.applicationContext

    val settings: StateFlow<AppSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** null while the first database read is still running. */
    val pdfs: StateFlow<List<PdfEntity>?> =
        repo.observePdfs().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val folders: StateFlow<List<FolderEntity>> =
        repo.observeFolders().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val progress = repo.progress
    val importing = repo.importing

    /** Set by search results to open a specific folder on the Folders tab. */
    val pendingFolder = MutableStateFlow<Long?>(null)

    val filterMode = MutableStateFlow(FilterMode.ALL)
    val dateFilter = MutableStateFlow(DateFilter.ANY)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()
    private fun say(msg: String) {
        _events.tryEmit(msg)
    }

    // ------------------------------------------------------------ import

    fun importFolder(uri: Uri) = viewModelScope.launch {
        val n = runCatching { repo.importTree(uri) }.getOrElse {
            say("Could not read that folder")
            return@launch
        }
        say(if (n == 0) "No new PDFs found" else "Added $n PDF${if (n == 1) "" else "s"}")
    }

    fun importFiles(uris: List<Uri>) = viewModelScope.launch {
        if (uris.isEmpty()) return@launch
        val n = runCatching { repo.importFiles(uris) }.getOrDefault(0)
        say(if (n == 0) "Those PDFs are already in your library" else "Added $n PDF${if (n == 1) "" else "s"}")
    }

    fun rescan() = viewModelScope.launch {
        val n = runCatching { repo.rescanAll() }.getOrDefault(0)
        say(if (n == 0) "Library is up to date" else "Found $n new PDF${if (n == 1) "" else "s"}")
    }

    fun removeSource(s: String) = viewModelScope.launch { repo.removeSource(s) }

    // ------------------------------------------------------------ PDF actions

    fun rename(pdf: PdfEntity, title: String, alsoRenameFile: Boolean) = viewModelScope.launch {
        val clean = title.trim()
        if (clean.isEmpty()) return@launch
        val fileTitle = pdf.fileName.removeSuffix(".pdf").removeSuffix(".PDF")
        if (alsoRenameFile) {
            val ok = repo.renameFile(pdf.id, clean)
            if (ok) repo.setTitle(pdf.id, null) else say("Couldn’t rename the file on the device — only the title was changed")
            if (!ok) repo.setTitle(pdf.id, clean)
        } else {
            repo.setTitle(pdf.id, if (clean == fileTitle) null else clean)
        }
    }

    fun setFavorite(ids: Collection<Long>, fav: Boolean) = viewModelScope.launch { repo.setFavorite(ids, fav) }
    fun move(ids: Collection<Long>, folderId: Long?) = viewModelScope.launch {
        repo.movePdfs(ids, folderId)
        say("Moved ${ids.size} PDF${if (ids.size == 1) "" else "s"}")
    }

    fun removeFromLibrary(ids: Collection<Long>) = viewModelScope.launch {
        repo.removeFromLibrary(ids)
        say("Removed from library — files are untouched")
    }

    fun deleteFiles(ids: Collection<Long>) = viewModelScope.launch {
        val failed = repo.deleteFiles(ids)
        say(if (failed == 0) "Deleted from device" else "$failed file(s) could not be deleted (no permission)")
    }

    fun setCover(id: Long, uri: Uri) = viewModelScope.launch {
        if (!repo.setPdfCover(id, uri)) say("Couldn’t use that image")
    }

    fun restoreCover(id: Long) = viewModelScope.launch { repo.restorePdfCover(id) }
    fun markOpened(id: Long) = viewModelScope.launch { repo.markOpened(id) }
    fun clearHistory() = viewModelScope.launch { repo.clearHistory() }
    fun setManualOrder(ids: List<Long>) = viewModelScope.launch { repo.setManualOrder(ids) }

    // ------------------------------------------------------------ folders

    fun createFolder(name: String, parentId: Long?, thenMove: Collection<Long> = emptyList()) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val id = repo.createFolder(name, parentId)
        if (thenMove.isNotEmpty()) repo.movePdfs(thenMove, id)
    }

    fun renameFolder(id: Long, name: String) = viewModelScope.launch { if (name.isNotBlank()) repo.renameFolder(id, name) }
    fun deleteFolder(id: Long) = viewModelScope.launch {
        repo.deleteFolder(id)
        say("Folder deleted — its PDFs moved up one level")
    }

    fun moveFolder(id: Long, parent: Long?) = viewModelScope.launch {
        if (!repo.moveFolder(id, parent)) say("A folder can’t be moved into itself")
    }

    fun setFolderCover(id: Long, uri: Uri) = viewModelScope.launch {
        if (!repo.setFolderCover(id, uri)) say("Couldn’t use that image")
    }

    fun restoreFolderCover(id: Long) = viewModelScope.launch { repo.restoreFolderCover(id) }

    // ------------------------------------------------------------ settings

    fun setLayout(v: LayoutMode) = viewModelScope.launch { store.setLayout(v) }
    fun setTheme(v: ThemeMode) = viewModelScope.launch { store.setTheme(v) }
    fun setSort(f: SortField, desc: Boolean) = viewModelScope.launch { store.setSort(f, desc) }
    fun setShowTitles(v: Boolean) = viewModelScope.launch { store.setShowTitles(v) }
    fun setShowBadge(v: Boolean) = viewModelScope.launch { store.setShowBadge(v) }
    fun setCoverStyle(v: CoverStyle) = viewModelScope.launch { store.setCoverStyle(v) }
    fun setArrangeConfidence(v: Float) = viewModelScope.launch { store.setArrangeConfidence(v) }
    fun setArrangeGroupSize(v: Int) = viewModelScope.launch { store.setArrangeGroupSize(v) }
    fun setArrangeUnfiled(v: Boolean) = viewModelScope.launch { store.setArrangeUnfiled(v) }
    fun setArrangeContent(v: Boolean) = viewModelScope.launch { store.setArrangeContent(v) }

    // ------------------------------------------------------------ storage & backup

    val storageInfo = MutableStateFlow(StorageInfo())

    fun refreshStorage() = viewModelScope.launch {
        val info = withContext(Dispatchers.IO) {
            StorageInfo(
                thumbBytes = thumbs.cacheSizeBytes(),
                coverBytes = thumbs.customCoverSizeBytes(),
                dbBytes = repo.databaseSizeBytes()
            )
        }
        storageInfo.value = info
    }

    fun clearThumbnailCache() = viewModelScope.launch {
        repo.resetThumbnails()
        say("Thumbnail cache cleared")
        refreshStorage()
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        val ok = runCatching {
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { container.backup.export(it) } != null
            }
        }.getOrDefault(false)
        say(if (ok) "Backup saved" else "Backup failed")
    }

    fun restoreBackup(uri: Uri) = viewModelScope.launch {
        val msg = runCatching {
            withContext(Dispatchers.IO) {
                val input = context.contentResolver.openInputStream(uri) ?: error("no input")
                input.use { container.backup.restore(it) }
            }
        }.getOrElse { "Restore failed: ${it.message ?: "invalid file"}" }
        say(msg)
    }
}

data class StorageInfo(val thumbBytes: Long = 0, val coverBytes: Long = 0, val dbBytes: Long = 0)
