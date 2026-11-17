package com.lumen.pdf.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumen.pdf.LumenApp
import com.lumen.pdf.data.Bookmark
import com.lumen.pdf.data.Folder
import com.lumen.pdf.data.LibraryLayout
import com.lumen.pdf.data.PdfFile
import com.lumen.pdf.data.ThemeMode
import com.lumen.pdf.pdf.AutoArrangeEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val lumen = app as LumenApp
    val repo = lumen.repo
    val settings = lumen.settings

    val pdfs: StateFlow<List<PdfFile>> = repo.observePdfs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<Folder>> = repo.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recent: StateFlow<List<PdfFile>> = repo.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<PdfFile>> = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val theme: StateFlow<ThemeMode> = settings.theme
    val layout: StateFlow<LibraryLayout> = settings.layout
    val showRecentStrip: StateFlow<Boolean> = settings.showRecentStrip
    val autoCovers: StateFlow<Boolean> = settings.autoCovers
    val manualOrder: StateFlow<Boolean> = settings.manualOrder
    val threshold: StateFlow<Float> = settings.autoArrangeThreshold

    val busy = MutableStateFlow(false)
    val arrangeResult = MutableStateFlow<AutoArrangeEngine.Result?>(null)
    val arrangeBusy = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    init {
        viewModelScope.launch { runCatching { repo.refreshMissing() } }
    }

    fun say(message: String) {
        _messages.tryEmit(message)
    }

    fun importTree(uri: Uri) = viewModelScope.launch {
        busy.value = true
        val count = runCatching { repo.importTree(uri) }.getOrDefault(0)
        busy.value = false
        say(if (count > 0) "Imported $count PDFs" else "No new PDFs found")
    }

    fun importUris(uris: List<Uri>) = viewModelScope.launch {
        if (uris.isEmpty()) return@launch
        busy.value = true
        val count = runCatching { repo.importUris(uris) }.getOrDefault(0)
        busy.value = false
        say(if (count > 0) "Imported $count PDFs" else "Already in library")
    }

    fun rename(pdfId: Long, title: String) = viewModelScope.launch {
        repo.rename(pdfId, title)
        say("Renamed")
    }

    fun toggleFavorite(pdf: PdfFile) = viewModelScope.launch {
        repo.setFavorite(pdf.id, !pdf.isFavorite)
    }

    fun deletePdfs(ids: List<Long>) = viewModelScope.launch {
        repo.deletePdfs(ids)
        say("Removed ${ids.size} item(s) from library")
    }

    fun moveToFolder(ids: List<Long>, folderId: Long?) = viewModelScope.launch {
        repo.moveToFolder(ids, folderId)
        say("Moved ${ids.size} item(s)")
    }

    fun setCustomCover(pdfId: Long, uri: Uri) = viewModelScope.launch {
        val ok = repo.setCustomCover(pdfId, uri)
        say(if (ok) "Cover updated" else "Could not read that image")
    }

    fun restoreCover(pdfId: Long) = viewModelScope.launch {
        val ok = repo.restoreCover(pdfId)
        say(if (ok) "Original cover restored" else "Could not restore cover")
    }

    fun setFolderCover(folderId: Long, uri: Uri) = viewModelScope.launch {
        val ok = repo.setFolderCover(folderId, uri)
        say(if (ok) "Folder cover updated" else "Could not read that image")
    }

    fun createFolder(name: String, parentId: Long?) = viewModelScope.launch {
        repo.createFolder(name, parentId)
        say("Folder created")
    }

    fun moveFolderInto(folderId: Long, parentId: Long?) = viewModelScope.launch {
        repo.moveFolder(folderId, parentId)
        say("Folder moved")
    }

    fun renameFolder(id: Long, name: String) = viewModelScope.launch {
        repo.renameFolder(id, name)
        say("Folder renamed")
    }

    fun deleteFolder(id: Long) = viewModelScope.launch {
        repo.deleteFolder(id)
        say("Folder deleted, contents moved up")
    }

    fun updateProgress(pdfId: Long, page: Int) = viewModelScope.launch {
        repo.updateProgress(pdfId, page)
    }

    fun toggleBookmark(pdfId: Long, page: Int) = viewModelScope.launch {
        repo.toggleBookmark(pdfId, page)
        say("Bookmark toggled for page $page")
    }

    fun bookmarks(pdfId: Long): StateFlow<List<Bookmark>> = repo.observeBookmarks(pdfId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), emptyList())

    fun persistOrder(ids: List<Long>) = viewModelScope.launch { repo.persistOrder(ids) }

    fun setTheme(mode: ThemeMode) = settings.setTheme(mode)
    fun setLayout(layout: LibraryLayout) = settings.setLayout(layout)
    fun setShowRecentStrip(value: Boolean) = settings.setShowRecentStrip(value)
    fun setAutoCovers(value: Boolean) = settings.setAutoCovers(value)
    fun setManualOrder(value: Boolean) = settings.setManualOrder(value)
    fun setThreshold(value: Float) = settings.setAutoArrangeThreshold(value)

    fun previewArrange() = viewModelScope.launch {
        arrangeBusy.value = true
        val result = runCatching { repo.buildArrangePreview(settings.autoArrangeThreshold.value) }
            .getOrNull()
        arrangeBusy.value = false
        if (result == null) {
            say("Could not analyse the library")
        } else if (result.proposals.isEmpty()) {
            say("No related groups found yet")
        } else {
            arrangeResult.value = result
        }
    }

    fun applyArrange(result: AutoArrangeEngine.Result) = viewModelScope.launch {
        arrangeBusy.value = true
        runCatching { repo.applyArrange(result) }
        arrangeBusy.value = false
        arrangeResult.value = null
        say("Library organised")
    }

    fun dismissArrange() {
        arrangeResult.value = null
    }

    fun rescanLibrary() = viewModelScope.launch {
        busy.value = true
        runCatching { repo.rescanAll() }
        busy.value = false
        say("Metadata refreshed")
    }

    fun clearCache() = viewModelScope.launch {
        val freed = repo.clearThumbnailCache()
        say("Freed ${freed / 1024} KB of cache")
    }

    fun exportSettings(): String = settings.exportJson()

    fun importSettings(raw: String) {
        val ok = settings.importJson(raw)
        say(if (ok) "Settings restored" else "Invalid backup file")
    }
}
