package com.lumen.pdf.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream

/**
 * All low level PDF work: page counts, cover rendering (framework PdfRenderer) and
 * text extraction (PdfBox) used by the auto arrange engine and content search.
 */
object PdfTools {

    fun pageCount(context: Context, uri: Uri): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (t: Throwable) {
            0
        } finally {
            try { renderer?.close() } catch (_: Throwable) { }
            try { pfd?.close() } catch (_: Throwable) { }
        }
    }

    fun renderPage(context: Context, uri: Uri, pageIndex: Int, targetWidth: Int): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
            page = renderer.openPage(pageIndex)
            val width = targetWidth.coerceIn(160, 2048)
            val height = (width.toFloat() * page.height.toFloat() / page.width.toFloat())
                .toInt().coerceIn(1, 4096)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (t: Throwable) {
            null
        } finally {
            try { page?.close() } catch (_: Throwable) { }
            try { renderer?.close() } catch (_: Throwable) { }
            try { pfd?.close() } catch (_: Throwable) { }
        }
    }

    /** Renders page 0 and stores it as the cover for [pdfId]. Returns the file path. */
    fun saveCover(context: Context, uri: Uri, pdfId: Long, width: Int = 640): String? {
        val bitmap = renderPage(context, uri, 0, width) ?: return null
        return try {
            val dir = File(context.filesDir, "covers").apply { mkdirs() }
            val file = File(dir, "cover_$pdfId.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 92, out)
            }
            bitmap.recycle()
            file.absolutePath
        } catch (t: Throwable) {
            try { bitmap.recycle() } catch (_: Throwable) { }
            null
        }
    }

    /** First [maxPages] pages as plain text, used for snippet, keywords and search. */
    fun extractText(context: Context, uri: Uri, maxPages: Int = 3): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val document = PDDocument.load(input)
                try {
                    val stripper = PDFTextStripper()
                    val pages = document.numberOfPages
                    if (pages <= 0) {
                        ""
                    } else {
                        stripper.startPage = 1
                        stripper.endPage = minOf(maxPages, pages)
                        stripper.getText(document)
                    }
                } finally {
                    try { document.close() } catch (_: Throwable) { }
                }
            } ?: ""
        } catch (t: Throwable) {
            ""
        }
    }

    /** Page numbers (1 based) whose text contains [query], capped for speed. */
    fun searchPages(context: Context, uri: Uri, query: String, maxPages: Int = 60): List<Int> {
        if (query.isBlank()) return emptyList()
        val needle = query.trim().lowercase()
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val document = PDDocument.load(input)
                try {
                    val stripper = PDFTextStripper()
                    val hits = ArrayList<Int>()
                    val limit = minOf(maxPages, document.numberOfPages)
                    for (p in 1..limit) {
                        stripper.startPage = p
                        stripper.endPage = p
                        val text = try { stripper.getText(document) } catch (_: Throwable) { "" }
                        if (text.lowercase().contains(needle)) hits.add(p)
                    }
                    hits
                } finally {
                    try { document.close() } catch (_: Throwable) { }
                }
            } ?: emptyList()
        } catch (t: Throwable) {
            emptyList()
        }
    }
}
