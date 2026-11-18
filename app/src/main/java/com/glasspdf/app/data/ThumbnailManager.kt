package com.glasspdf.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Lazily renders, caches (memory + disk) and serves PDF covers and custom cover images. */
class ThumbnailManager(private val context: Context) {

    private val thumbDir = File(context.cacheDir, "thumbs").apply { mkdirs() }
    private val coverDir = File(context.filesDir, "covers").apply { mkdirs() }
    private val gate = Semaphore(2)

    private val memory = object : LruCache<String, Bitmap>(
        ((Runtime.getRuntime().maxMemory() / 1024L) / 8L).toInt().coerceAtLeast(4096)
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    suspend fun cover(pdf: PdfEntity): Bitmap? = withContext(Dispatchers.IO) {
        val custom = pdf.customCoverPath
        if (custom != null && File(custom).exists()) {
            return@withContext fromFile(custom)
        }
        val key = "pdf:${pdf.id}"
        memory.get(key)?.let { return@withContext it }
        val file = File(thumbDir, "${pdf.id}.jpg")
        if (file.exists()) {
            val bmp = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            if (bmp != null) {
                memory.put(key, bmp)
                return@withContext bmp
            }
        }
        gate.withPermit {
            memory.get(key) ?: generate(pdf, file)?.also { memory.put(key, it) }
        }
    }

    suspend fun image(path: String): Bitmap? = withContext(Dispatchers.IO) { fromFile(path) }

    private fun fromFile(path: String): Bitmap? {
        val key = "file:$path"
        memory.get(key)?.let { return it }
        val bmp = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return null
        memory.put(key, bmp)
        return bmp
    }

    private fun generate(pdf: PdfEntity, target: File): Bitmap? = try {
        context.contentResolver.openFileDescriptor(Uri.parse(pdf.uri), "r")?.use { fd ->
            PdfRenderer(fd).use { renderer ->
                if (renderer.pageCount == 0) return@use null
                renderer.openPage(0).use { page ->
                    val w = THUMB_WIDTH
                    val h = (page.height.toFloat() / page.width * w).toInt().coerceIn(1, 2400)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    runCatching {
                        FileOutputStream(target).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 86, out) }
                    }
                    bmp
                }
            }
        }
    } catch (t: Throwable) {
        null
    }

    /** Copies a picked image into app storage (downscaled) and returns the new file path. */
    suspend fun importCover(source: Uri, name: String): String? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return@withContext null
            val file = File(coverDir, "${name}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            file.absolutePath
        } catch (t: Throwable) {
            null
        }
    }

    fun deleteCoverFile(path: String?) {
        if (path == null) return
        runCatching { File(path).delete() }
        memory.remove("file:$path")
    }

    fun cacheSizeBytes(): Long = thumbDir.listFiles()?.sumOf { it.length() } ?: 0L

    fun customCoverSizeBytes(): Long = coverDir.listFiles()?.sumOf { it.length() } ?: 0L

    fun clearCache(): Long {
        val freed = cacheSizeBytes()
        thumbDir.listFiles()?.forEach { runCatching { it.delete() } }
        memory.evictAll()
        return freed
    }

    companion object {
        const val THUMB_WIDTH = 480
    }
}
