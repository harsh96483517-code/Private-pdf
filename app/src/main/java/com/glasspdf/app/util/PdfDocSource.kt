package com.glasspdf.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** Thin wrapper over [PdfRenderer]: one page at a time, bitmap cache, safe to close while rendering. */
class PdfDocSource(context: Context, uri: Uri) : Closeable {
    private val pfd: ParcelFileDescriptor =
        context.contentResolver.openFileDescriptor(uri, "r") ?: throw IOException("Cannot open file")

    private val renderer: PdfRenderer = try {
        PdfRenderer(pfd)
    } catch (t: Throwable) {
        runCatching { pfd.close() }
        throw IOException("Cannot read PDF", t)
    }

    val pageCount: Int = renderer.pageCount

    private val mutex = Mutex()
    private val sizes = ConcurrentHashMap<Int, Pair<Int, Int>>()

    @Volatile
    private var closed = false

    private val cache = object : LruCache<String, Bitmap>(
        ((Runtime.getRuntime().maxMemory() / 1024L) / 5L).toInt().coerceAtLeast(8192)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    init {
        if (pageCount > 0) {
            runCatching { renderer.openPage(0).use { sizes[0] = it.width to it.height } }
        }
    }

    fun cached(index: Int, widthPx: Int): Bitmap? = cache.get("$index@$widthPx")

    /** Height / width of a page, when already known. */
    fun aspect(index: Int): Float? = sizes[index]?.let { it.second.toFloat() / it.first.toFloat() }

    suspend fun render(index: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$index@$widthPx"
        val hit = cache.get(key)
        if (hit != null) {
            hit
        } else {
            mutex.withLock {
                val again = cache.get(key)
                if (again != null) {
                    again
                } else if (closed || index !in 0 until pageCount) {
                    null
                } else {
                    try {
                        renderer.openPage(index).use { page ->
                            sizes[index] = page.width to page.height
                            val w = widthPx.coerceIn(64, 3000)
                            val h = (page.height.toFloat() / page.width * w).toInt().coerceIn(1, 6000)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            cache.put(key, bmp)
                            bmp
                        }
                    } catch (t: Throwable) {
                        null
                    }
                }
            }
        }
    }

    override fun close() {
        closed = true
        // If a render is running, it finishes first; otherwise close right away.
        if (mutex.tryLock()) {
            try {
                runCatching { renderer.close() }
                runCatching { pfd.close() }
            } finally {
                mutex.unlock()
            }
        }
        cache.evictAll()
    }
}
