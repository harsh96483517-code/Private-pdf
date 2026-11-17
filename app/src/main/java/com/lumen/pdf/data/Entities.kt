package com.lumen.pdf.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A PDF that lives in the user's own storage. The file is never copied: we keep the
 * SAF content uri and store all app level metadata in Room.
 */
@Entity(tableName = "pdfs")
data class PdfFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val fileName: String,
    val title: String,
    val folderId: Long? = null,
    val coverPath: String? = null,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val lastOpened: Long = 0L,
    val lastPage: Int = 0,
    val pageCount: Int = 0,
    val isFavorite: Boolean = false,
    val snippet: String = "",
    val keywords: String = "",
    val sortOrder: Int = 0
)

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val coverPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pdfId: Long,
    val page: Int,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
