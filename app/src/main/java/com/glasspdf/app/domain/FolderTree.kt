package com.glasspdf.app.domain

import com.glasspdf.app.data.FolderEntity
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.util.naturalCompare

object FolderTree {

    fun children(folders: List<FolderEntity>, parentId: Long?): List<FolderEntity> =
        folders.filter { it.parentId == parentId }.sortedWith(Comparator<FolderEntity> { a, b -> naturalCompare(a.name, b.name) })

    /** All folder ids below [id] (not including [id]). */
    fun descendants(folders: List<FolderEntity>, id: Long): Set<Long> {
        val out = HashSet<Long>()
        val byParent = folders.groupBy { it.parentId }
        val stack = ArrayDeque<Long>()
        stack.addLast(id)
        while (stack.isNotEmpty()) {
            val cur = stack.removeLast()
            for (c in byParent[cur].orEmpty()) {
                if (out.add(c.id)) stack.addLast(c.id)
            }
        }
        return out
    }

    /** Root → ... → [id] */
    fun path(folders: List<FolderEntity>, id: Long?): List<FolderEntity> {
        val byId = folders.associateBy { it.id }
        val out = ArrayList<FolderEntity>()
        var cur = id?.let { byId[it] }
        var guard = 0
        while (cur != null && guard < 64) {
            out.add(0, cur)
            cur = cur.parentId?.let { byId[it] }
            guard++
        }
        return out
    }

    /** Depth-first list with indentation level, skipping [exclude] folders and everything below them. */
    fun flatten(folders: List<FolderEntity>, exclude: Set<Long> = emptySet()): List<Pair<FolderEntity, Int>> {
        val out = ArrayList<Pair<FolderEntity, Int>>()
        fun walk(parent: Long?, depth: Int) {
            for (f in children(folders, parent)) {
                if (f.id in exclude) continue
                out.add(f to depth)
                walk(f.id, depth + 1)
            }
        }
        walk(null, 0)
        return out
    }

    /** PDFs inside [folderId], including nested folders. */
    fun pdfsRecursive(folders: List<FolderEntity>, pdfs: List<PdfEntity>, folderId: Long): List<PdfEntity> {
        val ids = descendants(folders, folderId) + folderId
        return pdfs.filter { it.folderId != null && it.folderId in ids }
    }
}
