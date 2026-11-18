package com.glasspdf.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

/** Exports / restores app settings and library organisation (folders, titles, favorites, bookmarks, progress) as JSON. */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val repo: LibraryRepository
) {
    suspend fun export(out: OutputStream) = withContext(Dispatchers.IO) {
        val s = settings.current()
        val root = JSONObject()
        root.put("app", "GlassPDF")
        root.put("version", 1)
        root.put("settings", JSONObject().apply {
            put("layout", s.layout.name)
            put("theme", s.theme.name)
            put("sortField", s.sortField.name)
            put("sortDescending", s.sortDescending)
            put("showTitles", s.showTitles)
            put("showPageBadge", s.showPageBadge)
            put("coverStyle", s.coverStyle.name)
            put("arrangeMinConfidence", s.arrangeMinConfidence.toDouble())
            put("arrangeMinGroupSize", s.arrangeMinGroupSize)
            put("arrangeOnlyUnfiled", s.arrangeOnlyUnfiled)
            put("arrangeUseContent", s.arrangeUseContent)
        })
        root.put("sources", JSONArray(s.sources.toList()))

        val folders = JSONArray()
        db.folderDao().getAll().forEach { f ->
            folders.put(JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("parentId", f.parentId ?: JSONObject.NULL)
                put("sortOrder", f.sortOrder)
            })
        }
        root.put("folders", folders)

        val pdfs = db.pdfDao().getAll()
        val pdfArr = JSONArray()
        pdfs.forEach { p ->
            pdfArr.put(JSONObject().apply {
                put("uri", p.uri)
                put("fileName", p.fileName)
                put("customTitle", p.customTitle ?: JSONObject.NULL)
                put("folderId", p.folderId ?: JSONObject.NULL)
                put("favorite", p.isFavorite)
                put("lastPage", p.lastPage)
                put("lastOpenedAt", p.lastOpenedAt)
                put("sortOrder", p.sortOrder)
                put("sizeBytes", p.sizeBytes)
                put("modifiedAt", p.modifiedAt)
            })
        }
        root.put("pdfs", pdfArr)

        val uriById = pdfs.associate { it.id to it.uri }
        val marks = JSONArray()
        db.bookmarkDao().getAll().forEach { b ->
            val uri = uriById[b.pdfId] ?: return@forEach
            marks.put(JSONObject().apply {
                put("pdfUri", uri)
                put("page", b.page)
                put("label", b.label)
            })
        }
        root.put("bookmarks", marks)
        out.write(root.toString(2).toByteArray(Charsets.UTF_8))
        out.flush()
    }

    /** Returns a short summary, or throws if the file is not a valid backup. */
    suspend fun restore(input: InputStream): String = withContext(Dispatchers.IO) {
        val root = JSONObject(input.readBytes().toString(Charsets.UTF_8))
        require(root.optString("app") == "GlassPDF") { "Not a GlassPDF backup" }

        root.optJSONObject("settings")?.let { o ->
            val d = AppSettings()
            fun <E : Enum<E>> parse(values: Array<E>, name: String?, default: E): E =
                values.firstOrNull { it.name == name } ?: default
            settings.setLayout(parse(LayoutMode.values(), o.optString("layout"), d.layout))
            settings.setTheme(parse(ThemeMode.values(), o.optString("theme"), d.theme))
            settings.setSort(
                parse(SortField.values(), o.optString("sortField"), d.sortField),
                o.optBoolean("sortDescending", d.sortDescending)
            )
            settings.setShowTitles(o.optBoolean("showTitles", d.showTitles))
            settings.setShowBadge(o.optBoolean("showPageBadge", d.showPageBadge))
            settings.setCoverStyle(parse(CoverStyle.values(), o.optString("coverStyle"), d.coverStyle))
            settings.setArrangeConfidence(o.optDouble("arrangeMinConfidence", d.arrangeMinConfidence.toDouble()).toFloat())
            settings.setArrangeGroupSize(o.optInt("arrangeMinGroupSize", d.arrangeMinGroupSize))
            settings.setArrangeUnfiled(o.optBoolean("arrangeOnlyUnfiled", d.arrangeOnlyUnfiled))
            settings.setArrangeContent(o.optBoolean("arrangeUseContent", d.arrangeUseContent))
        }

        // Re-attach folder sources that this device still has permission for.
        val persisted = context.contentResolver.persistedUriPermissions.map { it.uri.toString() }.toSet()
        root.optJSONArray("sources")?.let { arr ->
            val restored = (0 until arr.length()).map { arr.getString(it) }.filter { it in persisted }
            if (restored.isNotEmpty()) settings.setSources(settings.current().sources + restored)
        }

        var folderCount = 0
        var pdfCount = 0
        db.withTransaction {
            // Folders (parents first)
            val folderArr = root.optJSONArray("folders") ?: JSONArray()
            data class F(val id: Long, val name: String, val parent: Long?, val order: Int)
            val list = (0 until folderArr.length()).map { i ->
                val o = folderArr.getJSONObject(i)
                F(o.getLong("id"), o.getString("name"), if (o.isNull("parentId")) null else o.getLong("parentId"), o.optInt("sortOrder"))
            }
            db.pdfDao().getAll().map { it.id }.chunked(500).forEach { db.pdfDao().moveTo(it, null) }
            db.folderDao().deleteAll()
            val idMap = HashMap<Long, Long>()
            var remaining = list
            var guard = 0
            while (remaining.isNotEmpty() && guard < 64) {
                val (ready, later) = remaining.partition { it.parent == null || idMap.containsKey(it.parent) }
                if (ready.isEmpty()) break
                for (f in ready) {
                    val newId = db.folderDao().insert(
                        FolderEntity(name = f.name, parentId = f.parent?.let { idMap[it] }, sortOrder = f.order, createdAt = System.currentTimeMillis())
                    )
                    idMap[f.id] = newId
                    folderCount++
                }
                remaining = later
                guard++
            }

            // PDFs
            val existing = db.pdfDao().getAll().associateBy { it.uri }
            val pdfArr = root.optJSONArray("pdfs") ?: JSONArray()
            for (i in 0 until pdfArr.length()) {
                val o = pdfArr.getJSONObject(i)
                val uri = o.getString("uri")
                val folder = if (o.isNull("folderId")) null else idMap[o.getLong("folderId")]
                val title = if (o.isNull("customTitle")) null else o.getString("customTitle")
                val cur = existing[uri]
                if (cur != null) {
                    db.pdfDao().update(
                        cur.copy(
                            customTitle = title, folderId = folder, isFavorite = o.optBoolean("favorite"),
                            lastPage = o.optInt("lastPage"), lastOpenedAt = o.optLong("lastOpenedAt"),
                            sortOrder = o.optInt("sortOrder")
                        )
                    )
                } else {
                    db.pdfDao().insert(
                        PdfEntity(
                            uri = uri, fileName = o.optString("fileName", "document.pdf"), customTitle = title,
                            sizeBytes = o.optLong("sizeBytes"), modifiedAt = o.optLong("modifiedAt"),
                            addedAt = System.currentTimeMillis(), folderId = folder,
                            isFavorite = o.optBoolean("favorite"), lastPage = o.optInt("lastPage"),
                            lastOpenedAt = o.optLong("lastOpenedAt"), sortOrder = o.optInt("sortOrder")
                        )
                    )
                }
                pdfCount++
            }

            // Bookmarks
            db.bookmarkDao().deleteAll()
            val idByUri = db.pdfDao().getAll().associate { it.uri to it.id }
            val marks = root.optJSONArray("bookmarks") ?: JSONArray()
            for (i in 0 until marks.length()) {
                val o = marks.getJSONObject(i)
                val pid = idByUri[o.getString("pdfUri")] ?: continue
                db.bookmarkDao().insert(
                    BookmarkEntity(pdfId = pid, page = o.getInt("page"), label = o.optString("label"), createdAt = System.currentTimeMillis())
                )
            }
        }
        repo.startAnalysis()
        "Restored $folderCount folders and $pdfCount PDFs"
    }
}
