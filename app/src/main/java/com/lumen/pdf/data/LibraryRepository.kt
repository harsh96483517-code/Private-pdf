package com.lumen.pdf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.lumen.pdf.pdf.AutoArrangeEngine
import com.lumen.pdf.pdf.PdfTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LibraryRepository(context: Context, private val db: AppDatabase) {

    private val appContext = context.applicationContext
    private val pdfDao = db.pdfDao()
    private val folderDao = db.folderDao()
    private val bookmarkDao = db.bookmarkDao()

    private val coversDir = File(appContext.filesDir, "covers").apply { mkdirs() }

    fun observePdfs(): Flow<List<PdfFile>> = pdfDao.observeAll()
    fun observeFavorites(): Flow<List<PdfFile>> = pdfDao.observeFavorites()
    fun observeRecent(): Flow<List<PdfFile>> = pdfDao.observeRecent()
    fun observeFolders(): Flow<List<Folder>> = folderDao.observeAll()
    fun observeFolderChildren(parentId: Long?): Flow<List<Folder>> = folderDao.observeChildren(parentId)
    fun observePdfsInFolder(folderId: Long?): Flow<List<PdfFile>> = pdfDao.observeInFolder(folderId)
    fun observePdf(id: Long): Flow<PdfFile?> = pdfDao.observeById(id)
    fun observeBookmarks(pdfId: Long): Flow<List<Bookmark>> = bookmarkDao.observeForPdf(pdfId)

    suspend fun pdf(id: Long): PdfFile? = withContext(Dispatchers.IO) { pdfDao.byId(id) }
    suspend fun folder(id: Long): Folder? = withContext(Dispatchers.IO) { folderDao.byId(id) }
    suspend fun allFolders(): List<Folder> = withContext(Dispatchers.IO) { folderDao.all() }

    // ---------------------------------------------------------------- import

    /** Recursively imports every PDF under a SAF tree uri. Returns how many were new. */
    suspend fun importTree(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(appContext, treeUri) ?: return@withContext 0
        var added = 0
        val stack = ArrayDeque<DocumentFile>()
        stack.addLast(root)
        var guard = 0
        while (stack.isNotEmpty() && guard < 5000) {
            guard++
            val dir = stack.removeLast()
            val children = try { dir.listFiles() } catch (t: Throwable) { emptyArray() }
            children.forEach { child ->
                if (child.isDirectory) {
                    stack.addLast(child)
                } else if (isPdf(child)) {
                    if (insertIfNew(child)) added++
                }
            }
        }
        added
    }

    suspend fun importUris(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var added = 0
        uris.forEach { uri ->
            val doc = DocumentFile.fromSingleUri(appContext, uri)
            val name = doc?.name ?: uri.lastPathSegment ?: "Document.pdf"
            if (pdfDao.byUri(uri.toString()) == null) {
                val id = pdfDao.insert(
                    PdfFile(
                        uri = uri.toString(),
                        fileName = name,
                        title = name.removeSuffix(".pdf").removeSuffix(".PDF"),
                        size = doc?.length() ?: 0L,
                        lastModified = doc?.lastModified() ?: System.currentTimeMillis()
                    )
                )
                enrich(id)
                added++
            }
        }
        added
    }

    private fun isPdf(file: DocumentFile): Boolean {
        val name = file.name ?: return false
        return name.endsWith(".pdf", ignoreCase = true) || file.type == "application/pdf"
    }

    private suspend fun insertIfNew(file: DocumentFile): Boolean {
        val uri = file.uri.toString()
        if (pdfDao.byUri(uri) != null) return false
        val name = file.name ?: "Untitled.pdf"
        val id = pdfDao.insert(
            PdfFile(
                uri = uri,
                fileName = name,
                title = name.removeSuffix(".pdf").removeSuffix(".PDF"),
                size = file.length(),
                lastModified = file.lastModified()
            )
        )
        enrich(id)
        return true
    }

    /** Fills page count, cover and text snippet for one PDF. Safe to call repeatedly. */
    suspend fun enrich(id: Long) = withContext(Dispatchers.IO) {
        val pdf = pdfDao.byId(id) ?: return@withContext
        val uri = Uri.parse(pdf.uri)
        val pages = PdfTools.pageCount(appContext, uri)
        val cover = PdfTools.saveCover(appContext, uri, id)
        val text = PdfTools.extractText(appContext, uri, 2)
        pdfDao.update(
            pdf.copy(
                pageCount = pages,
                coverPath = cover ?: pdf.coverPath,
                snippet = text.take(4000),
                keywords = AutoArrangeEngine.keywordsOf("${pdf.title} ${pdf.fileName}", text)
            )
        )
    }

    /** Background scan for PDFs that still have no metadata (e.g. after a restore). */
    suspend fun refreshMissing() = withContext(Dispatchers.IO) {
        pdfDao.all().filter { it.pageCount == 0 || it.snippet.isEmpty() }.forEach { enrich(it.id) }
    }

    /** Re-reads page count, cover and text for every PDF in the library. */
    suspend fun rescanAll() = withContext(Dispatchers.IO) {
        pdfDao.all().forEach { enrich(it.id) }
    }

    suspend fun rename(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val pdf = pdfDao.byId(id) ?: return@withContext
        pdfDao.update(pdf.copy(title = newTitle.ifBlank { pdf.fileName }))
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = withContext(Dispatchers.IO) {
        pdfDao.setFavorite(id, favorite)
    }

    suspend fun deletePdfs(ids: List<Long>) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteForPdfs(ids)
        ids.forEach { id ->
            File(coversDir, "cover_$id.png").delete()
            File(coversDir, "custom_$id.png").delete()
        }
        pdfDao.deleteByIds(ids)
    }

    suspend fun moveToFolder(ids: List<Long>, folderId: Long?) = withContext(Dispatchers.IO) {
        val existing = pdfDao.all().filter { it.folderId == folderId }
        var order = existing.size
        ids.forEach { id ->
            pdfDao.setFolder(id, folderId, order++)
        }
    }

    suspend fun persistOrder(orderedIds: List<Long>) = withContext(Dispatchers.IO) {
        orderedIds.forEachIndexed { index, id -> pdfDao.setSortOrder(id, index) }
    }

    suspend fun updateProgress(id: Long, page: Int) = withContext(Dispatchers.IO) {
        val pdf = pdfDao.byId(id) ?: return@withContext
        pdfDao.update(pdf.copy(lastPage = page, lastOpened = System.currentTimeMillis()))
    }

    // ---------------------------------------------------------------- covers

    suspend fun setCustomCover(pdfId: Long, imageUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val path = copyImageToCovers(imageUri, "custom_$pdfId") ?: return@withContext false
        val pdf = pdfDao.byId(pdfId) ?: return@withContext false
        pdfDao.update(pdf.copy(coverPath = path))
        true
    }

    suspend fun restoreCover(pdfId: Long): Boolean = withContext(Dispatchers.IO) {
        val pdf = pdfDao.byId(pdfId) ?: return@withContext false
        File(coversDir, "custom_$pdfId.png").delete()
        val path = PdfTools.saveCover(appContext, Uri.parse(pdf.uri), pdfId) ?: return@withContext false
        pdfDao.update(pdf.copy(coverPath = path))
        true
    }

    suspend fun setFolderCover(folderId: Long, imageUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val path = copyImageToCovers(imageUri, "folder_$folderId") ?: return@withContext false
        val folder = folderDao.byId(folderId) ?: return@withContext false
        folderDao.update(folder.copy(coverPath = path))
        true
    }

    private fun copyImageToCovers(uri: Uri, baseName: String): String? = try {
        val bitmap: Bitmap? = appContext.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
        if (bitmap == null) null else {
            val file = File(coversDir, "$baseName.png")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 92, out) }
            bitmap.recycle()
            file.absolutePath
        }
    } catch (t: Throwable) {
        null
    }

    fun coversSizeBytes(): Long = coversDir.listFiles()?.sumOf { it.length() } ?: 0L

    fun clearThumbnailCache(): Long {
        var freed = 0L
        val dir = File(appContext.cacheDir, "pdfpages")
        dir.listFiles()?.forEach { file ->
            freed += file.length()
            file.delete()
        }
        return freed
    }

    // ---------------------------------------------------------------- folders

    suspend fun createFolder(name: String, parentId: Long?): Long = withContext(Dispatchers.IO) {
        folderDao.insert(Folder(name = name.ifBlank { "New folder" }, parentId = parentId))
    }

    suspend fun moveFolder(id: Long, parentId: Long?) = withContext(Dispatchers.IO) {
        val folder = folderDao.byId(id) ?: return@withContext
        if (folder.parentId == parentId) return@withContext
        folderDao.update(folder.copy(parentId = parentId))
    }

    suspend fun renameFolder(id: Long, name: String) = withContext(Dispatchers.IO) {
        val folder = folderDao.byId(id) ?: return@withContext
        folderDao.update(folder.copy(name = name.ifBlank { folder.name }))
    }

    suspend fun deleteFolder(id: Long) = withContext(Dispatchers.IO) {
        val folder = folderDao.byId(id) ?: return@withContext
        val parent = folder.parentId
        folderDao.reparentChildren(id, parent)
        val inside = pdfDao.all().filter { it.folderId == id }
        inside.forEachIndexed { index, pdf -> pdfDao.setFolder(pdf.id, parent, index) }
        File(coversDir, "folder_$id.png").delete()
        folderDao.delete(id)
    }

    suspend fun folderPath(folderId: Long?): List<Folder> = withContext(Dispatchers.IO) {
        val path = ArrayList<Folder>()
        var current = folderId
        var guard = 0
        while (current != null && guard < 40) {
            guard++
            val folder = folderDao.byId(current) ?: break
            path.add(0, folder)
            current = folder.parentId
        }
        path
    }

    // -------------------------------------------------------------- bookmarks

    suspend fun toggleBookmark(pdfId: Long, page: Int) = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.find(pdfId, page)
        if (existing != null) bookmarkDao.delete(existing.id)
        else bookmarkDao.insert(Bookmark(pdfId = pdfId, page = page))
    }

    // ------------------------------------------------------------ auto arrange

    suspend fun buildArrangePreview(threshold: Float): AutoArrangeEngine.Result =
        withContext(Dispatchers.Default) {
            val docs = pdfDao.all().map {
                AutoArrangeEngine.DocInput(
                    id = it.id,
                    name = "${it.title} ${it.fileName}",
                    text = it.snippet
                )
            }
            AutoArrangeEngine.arrange(docs, threshold)
        }

    suspend fun applyArrange(result: AutoArrangeEngine.Result) = withContext(Dispatchers.IO) {
        result.proposals.forEach { proposal ->
            val folderId = folderDao.findByName(proposal.name)?.id
                ?: folderDao.insert(Folder(name = proposal.name))
            proposal.ordered.forEachIndexed { index, pdfId ->
                val pdf = pdfDao.byId(pdfId) ?: return@forEachIndexed
                pdfDao.update(pdf.copy(folderId = folderId, sortOrder = index))
            }
        }
        if (result.unsorted.isNotEmpty()) {
            val folderId = folderDao.findByName("Unsorted")?.id
                ?: folderDao.insert(Folder(name = "Unsorted"))
            result.unsorted.forEachIndexed { index, pdfId ->
                val pdf = pdfDao.byId(pdfId) ?: return@forEachIndexed
                pdfDao.update(pdf.copy(folderId = folderId, sortOrder = index))
            }
        }
    }

    suspend fun searchInPdf(pdfId: Long, query: String): List<Int> = withContext(Dispatchers.IO) {
        val pdf = pdfDao.byId(pdfId) ?: return@withContext emptyList()
        PdfTools.searchPages(appContext, Uri.parse(pdf.uri), query)
    }
}
