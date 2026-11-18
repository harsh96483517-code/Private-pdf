package com.glasspdf.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.glasspdf.app.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ShareUtil {

    /** Copies the PDF to the app cache (temporary) and hands it to the Android share sheet. */
    suspend fun sharePdf(context: Context, uri: Uri, name: String): Boolean {
        val file = withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                dir.listFiles()?.forEach { runCatching { it.delete() } }
                var safe = name.replace(Regex("[^A-Za-z0-9._ -]"), "_").ifBlank { "document" }
                if (!safe.endsWith(".pdf", ignoreCase = true)) safe += ".pdf"
                val out = File(dir, safe)
                val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
                input.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
                out
            } catch (t: Throwable) {
                null
            }
        } ?: return false
        return try {
            val shared = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, shared)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Share PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (t: Throwable) {
            false
        }
    }

    /** Opens the PDF in another installed PDF app (this app is excluded from the chooser). */
    fun openWithExternal(context: Context, uri: Uri): Boolean = try {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(view, "Open with").apply {
            putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(context, MainActivity::class.java)))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    } catch (t: Throwable) {
        false
    }
}
