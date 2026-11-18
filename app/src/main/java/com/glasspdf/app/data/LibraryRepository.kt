package com.glasspdf.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import com.glasspdf.app.domain.FolderTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

data class ApplyGroup(val name: String, val pdfIds: List<Long>)

class LibraryRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore,
    val thumbs: ThumbnailManager,
    private val scope: CoroutineScope
) {
    private val importer = PdfImporter(context)
    private val analyzer = PdfAnalyzer(context)
    private val pdfDao = db.pdfDao()
    private val textDao = db.textDao()
    private val folderDao = db.folderDao()
    private val bookmarkDao = db.bookmarkDao()
    private val analysisMutex = Mutex()

    private val _progress = MutableStateFlow<ScanProgress?>(null)
    val progress: StateFlow<ScanProgress?> = _progress.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    fun observePdfs(): Flow<List<PdfEntity>> = pdfDao.observeAll()
    fun observeFolders(): Flow<List<FolderEntity>> = folderDao.observeAll()
    fun observePdf(id: Long): Flow<PdfEntity?> = pdfDao.observe(id)
    fun observeBookmarks(pdfId: Long): Flow<List<BookmarkEntity>> = bookmarkDao.observeFor(pdfId)
    suspend fun getPdf(id: Long): PdfEntity? = pdfDao.get(id)
    suspend fun allPdfs(): List<PdfEntity> = pdfDao.getAll()
    suspend fun allFolders(): List<FolderEntity> = folderDao.getAll()

    // ------------------------------------------------------------ import & scan

    private fun persist(uri: Uri) {
        val resolver = context.contentResolver
        try {
            resolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    suspend fun importTree(tree: Uri): Int {
        persist(tree)
        settings.setSources(settings.current().sources + tree.toString())
        return scanSource(tree)
    }

    suspend fun importFiles(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        uris.forEach { persist(it) }
        insertNew(uris.mapNotNull { importer.describe(it) })
    }

    private suspend fun scanSource(tree: Uri): Int = withContext(Dispatchers.IO) {
        _importing.value = true
        try {
            insertNew(importer.scanTree(tree))
        } finally {
            _importing.value = false
        }
    }

    suspend fun rescanAll(): Int {
        var total = 0
        for (s in settings.current().sources) {
            total += runCatching { scanSource(Uri.parse(s)) }.getOrDefault(0)
        }
        startAnalysis()
        return total
    }

    suspend fun removeSource(source: String) {
        settings.setSources(settings.current().sources - source)
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(source), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    private suspend fun insertNew(found: List<FoundPdf>): Int {
        val existing = pdfDao.allUris().toHashSet()
        val now = System.currentTimeMillis()
        val fresh = found.filter { it.uri.toString() !in existing }.map {
            PdfEntity(
                uri = it.uri.toString(), fileName = it.name, sizeBytes = it.size,
                modifiedAt = it.modified, addedAt = now
            )
        }
        fresh.chunked(200).forEach { pdfDao.insertAll(it) }
        if (fresh.isNotEmpty()) startAnalysis()
        return fresh.size
    }

    /** Background pass: page count, embedded title and text of the first pages. Never blocks the UI. */
    fun startAnalysis() {
        scope.launch(Dispatchers.IO) {
            if (!analysisMutex.tryLock()) return@launch
            try {
                while (true) {
                    val pending = pdfDao.pending()
                    if (pending.isEmpty()) break
                    pending.forEachIndexed { i, p ->
                        _progress.value = ScanProgress(i, pending.size, p.fileName)
                        try {
                            val r = analyzer.analyze(Uri.parse(p.uri), p.sizeBytes)
                            if (pdfDao.get(p.id) != null) {
                                db.withTransaction {
                                    pdfDao.setAnalysis(p.id, r.pageCount, r.metaTitle, r.broken)
                                    if (r.text.isNotBlank()) textDao.upsert(PdfTextEntity(p.id, r.text))
                                }
                            }
                        } catch (t: Throwable) {
                            runCatching { pdfDao.setAnalysis(p.id, 0, null, true) }
                        }
                    }
                }
            } finally {
                _progress.value = null
                analysisMutex.unlock()
            }
        }
    }

    // ------------------------------------------------------------ PDF edits

    suspend fun setTitle(id: Long, title: String?) = pdfDao.setTitle(id, title?.trim()?.takeIf { it.isNotEmpty() })

    /** Renames the real file on the device (only when the user asks for it). */
    suspend fun renameFile(id: Long, newName: String): Boolean = withContext(Dispatchers.IO) {
        val p = pdfDao.get(id) ?: return@withContext false
        val name = if (newName.endsWith(".pdf", ignoreCase = true)) newName else "$newName.pdf"
        try {
            val newUri = DocumentsContract.renameDocument(context.contentResolver, Uri.parse(p.uri), name)
            if (newUri != null) {
                pdfDao.setLocation(id, newUri.toString(), name)
                true
            } else false
        } catch (t: Throwable) {
            false
        }
    }

    suspend fun setPdfCover(id: Long, source: Uri): Boolean {
        val p = pdfDao.get(id) ?: return false
        val path = thumbs.importCover(source, "pdf_$id") ?: return false
        thumbs.deleteCoverFile(p.customCoverPath)
        pdfDao.setCover(id, path)
        return true
    }

    suspend fun restorePdfCover(id: Long) {
        val p = pdfDao.get(id) ?: return
        thumbs.deleteCoverFile(p.customCoverPath)
        pdfDao.setCover(id, null)
    }

    suspend fun setFavorite(ids: Collection<Long>, fav: Boolean) =
        ids.toList().chunked(500).forEach { pdfDao.setFavorite(it, fav) }

    suspend fun movePdfs(ids: Collection<Long>, folderId: Long?) =
        ids.toList().chunked(500).forEach { pdfDao.moveTo(it, folderId) }

    suspend fun setManualOrder(idsInOrder: List<Long>) {
        db.withTransaction { idsInOrder.forEachIndexed { i, id -> pdfDao.setOrder(id, i) } }
    }

    /** Removes entries from the library only; the files on the device stay untouched. */
    suspend fun removeFromLibrary(ids: Collection<Long>) {
        val all = pdfDao.getAll().filter { it.id in ids }
        all.forEach { thumbs.deleteCoverFile(it.customCoverPath) }
        ids.toList().chunked(500).forEach { pdfDao.deleteByIds(it) }
    }

    /** Deletes the real files. Returns how many could not be deleted. */
    suspend fun deleteFiles(ids: Collection<Long>): Int = withContext(Dispatchers.IO) {
        var failed = 0
        val targets = pdfDao.getAll().filter { it.id in ids }
        val deleted = ArrayList<Long>()
        for (p in targets) {
            val ok = try {
                DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(p.uri))
            } catch (t: Throwable) {
                false
            }
            if (ok) deleted.add(p.id) else failed++
        }
        if (deleted.isNotEmpty()) removeFromLibrary(deleted)
        failed
    }

    suspend fun markOpened(id: Long) = pdfDao.setOpened(id, System.currentTimeMillis())
    suspend fun saveLastPage(id: Long, page: Int) = pdfDao.setLastPage(id, page)
    suspend fun clearHistory() = pdfDao.clearHistory()

    // ------------------------------------------------------------ folders

    suspend fun createFolder(name: String, parentId: Long?): Long =
        folderDao.insert(FolderEntity(name = name.trim(), parentId = parentId, createdAt = System.currentTimeMillis()))

    suspend fun renameFolder(id: Long, name: String) = folderDao.rename(id, name.trim())

    /** PDFs and sub-folders move up one level; nothing is deleted. */
    suspend fun deleteFolder(id: Long) {
        db.withTransaction {
            val f = folderDao.get(id) ?: return@withTransaction
            pdfDao.reparentPdfs(id, f.parentId)
            folderDao.reparentChildren(id, f.parentId)
            folderDao.delete(id)
            thumbs.deleteCoverFile(f.customCoverPath)
        }
    }

    suspend fun moveFolder(id: Long, newParent: Long?): Boolean {
        val all = folderDao.getAll()
        if (newParent != null && (newParent == id || newParent in FolderTree.descendants(all, id))) return false
        folderDao.setParent(id, newParent)
        return true
    }

    suspend fun setFolderCover(id: Long, source: Uri): Boolean {
        val f = folderDao.get(id) ?: return false
        val path = thumbs.importCover(source, "folder_$id") ?: return false
        thumbs.deleteCoverFile(f.customCoverPath)
        folderDao.setCover(id, path)
        return true
    }

    suspend fun restoreFolderCover(id: Long) {
        val f = folderDao.get(id) ?: return
        thumbs.deleteCoverFile(f.customCoverPath)
        folderDao.setCover(id, null)
    }

    // ------------------------------------------------------------ bookmarks

    suspend fun toggleBookmark(pdfId: Long, page: Int, existing: Boolean) {
        if (existing) bookmarkDao.deleteAt(pdfId, page)
        else bookmarkDao.insert(BookmarkEntity(pdfId = pdfId, page = page, label = "Page ${page + 1}", createdAt = System.currentTimeMillis()))
    }

    suspend fun deleteBookmark(id: Long) = bookmarkDao.delete(id)

    // ------------------------------------------------------------ search / arrange support

    suspend fun searchContent(query: String): Map<Long, String> {
        if (query.length < 2) return emptyMap()
        return textDao.search(query).associate { it.pdfId to snippet(it.text, query) }
    }

    suspend fun textsFor(ids: List<Long>): Map<Long, String> {
        val out = HashMap<Long, String>()
        ids.chunked(500).forEach { chunk -> textDao.getFor(chunk).forEach { out[it.pdfId] = it.text } }
        return out
    }

    /** Creates the folders (or re-uses root folders with the same name) and files PDFs into them. Files on disk are never touched. */
    suspend fun applyArrangement(groups: List<ApplyGroup>): Int = db.withTransaction {
        val all = folderDao.getAll().toMutableList()
        val now = System.currentTimeMillis()
        var moved = 0
        for (g in groups) {
            if (g.pdfIds.isEmpty()) continue
            val existing = all.firstOrNull { it.parentId == null && it.name.equals(g.name, ignoreCase = true) }
            val folderId = existing?.id ?: run {
                val newId = folderDao.insert(FolderEntity(name = g.name, createdAt = now))
                all.add(FolderEntity(id = newId, name = g.name, createdAt = now))
                newId
            }
            g.pdfIds.forEachIndexed { idx, id ->
                pdfDao.moveTo(listOf(id), folderId)
                pdfDao.setOrder(id, idx)
                moved++
            }
        }
        moved
    }

    // ------------------------------------------------------------ storage

    suspend fun resetThumbnails(): Long = withContext(Dispatchers.IO) { thumbs.clearCache() }

    fun databaseSizeBytes(): Long = context.getDatabasePath("glasspdf.db").let { f ->
        if (f.exists()) f.length() else 0L
    }

    private fun snippet(text: String, query: String): String {
        val i = text.indexOf(query, ignoreCase = true)
        if (i < 0) return text.take(100)
        val start = (i - 40).coerceAtLeast(0)
        val end = (i + query.length + 60).coerceAtMost(text.length)
        return (if (start > 0) "…" else "") + text.substring(start, end) + (if (end < text.length) "…" else "")
    }
}
