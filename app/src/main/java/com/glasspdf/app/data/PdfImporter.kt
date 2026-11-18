package com.glasspdf.app.data

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

data class FoundPdf(val uri: Uri, val name: String, val size: Long, val modified: Long)

/** Finds PDFs through the Storage Access Framework. Nothing is copied. */
class PdfImporter(private val context: Context) {

    fun scanTree(tree: Uri): List<FoundPdf> {
        val out = ArrayList<FoundPdf>()
        val resolver = context.contentResolver
        val stack = ArrayDeque<String>()
        stack.addLast(DocumentsContract.getTreeDocumentId(tree))
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        var visited = 0
        while (stack.isNotEmpty() && visited < 50_000) {
            val parent = stack.removeLast()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
            runCatching {
                resolver.query(children, columns, null, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        visited++
                        val id = c.getString(0) ?: continue
                        val name = c.getString(1) ?: continue
                        val mime = c.getString(2) ?: ""
                        val size = if (c.isNull(3)) 0L else c.getLong(3)
                        val modified = if (c.isNull(4)) 0L else c.getLong(4)
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            if (!name.startsWith(".")) stack.addLast(id)
                        } else if (!name.startsWith(".") &&
                            (mime == "application/pdf" || name.endsWith(".pdf", ignoreCase = true))
                        ) {
                            out.add(FoundPdf(DocumentsContract.buildDocumentUriUsingTree(tree, id), name, size, modified))
                        }
                    }
                }
            }
        }
        return out
    }

    fun describe(uri: Uri): FoundPdf? = runCatching {
        var name = "document.pdf"
        var size = 0L
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    c.getString(0)?.let { name = it }
                    if (!c.isNull(1)) size = c.getLong(1)
                }
            }
        var modified = 0L
        runCatching {
            context.contentResolver.query(
                uri, arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null
            )?.use { c -> if (c.moveToFirst() && !c.isNull(0)) modified = c.getLong(0) }
        }
        FoundPdf(uri, name, size, if (modified > 0) modified else System.currentTimeMillis())
    }.getOrNull()
}

data class AnalysisResult(val pageCount: Int, val metaTitle: String?, val text: String, val broken: Boolean)

/** Reads page count, embedded title and the text of the first pages (for Auto Arrange and search). */
class PdfAnalyzer(private val context: Context) {

    fun analyze(uri: Uri, sizeBytes: Long): AnalysisResult {
        var pages = 0
        var broken = false
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd == null) {
                broken = true
            } else {
                pfd.use { fd ->
                    PdfRenderer(fd).use { r -> pages = r.pageCount }
                }
            }
        } catch (t: Throwable) {
            broken = true
        }
        if (broken) return AnalysisResult(0, null, "", true)

        var meta: String? = null
        var text = ""
        // Very large files are indexed by name only to stay light on RAM and battery.
        if (sizeBytes in 0..MAX_TEXT_BYTES) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val doc = PDDocument.load(input, MemoryUsageSetting.setupTempFileOnly())
                    try {
                        meta = doc.documentInformation?.title?.trim()?.takeIf { it.isNotEmpty() }
                        val stripper = PDFTextStripper()
                        stripper.startPage = 1
                        stripper.endPage = minOf(TEXT_PAGES, doc.numberOfPages)
                        text = stripper.getText(doc).replace(Regex("\\s+"), " ").trim().take(MAX_TEXT_CHARS)
                    } finally {
                        runCatching { doc.close() }
                    }
                }
            }
        }
        return AnalysisResult(pages, meta, text, false)
    }

    companion object {
        const val MAX_TEXT_BYTES = 150L * 1024 * 1024
        const val TEXT_PAGES = 3
        const val MAX_TEXT_CHARS = 8000
    }
}
