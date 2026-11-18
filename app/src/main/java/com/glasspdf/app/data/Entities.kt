package com.glasspdf.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pdfs",
    indices = [Index(value = ["uri"], unique = true), Index(value = ["folderId"])]
)
data class PdfEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val fileName: String,
    val customTitle: String? = null,
    val sizeBytes: Long = 0,
    val modifiedAt: Long = 0,
    val addedAt: Long = 0,
    val pageCount: Int = 0,
    val folderId: Long? = null,
    val customCoverPath: String? = null,
    val isFavorite: Boolean = false,
    val lastOpenedAt: Long = 0,
    val lastPage: Int = 0,
    val sortOrder: Int = 0,
    val metaTitle: String? = null,
    val analyzed: Boolean = false,
    val broken: Boolean = false
)

/** Title shown in the UI: custom title, otherwise the file name without extension. */
val PdfEntity.title: String
    get() = customTitle?.takeIf { it.isNotBlank() } ?: fileName.removeSuffix(".pdf").removeSuffix(".PDF")

@Entity(
    tableName = "pdf_text",
    foreignKeys = [ForeignKey(
        entity = PdfEntity::class,
        parentColumns = ["id"],
        childColumns = ["pdfId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PdfTextEntity(
    @PrimaryKey val pdfId: Long,
    val text: String
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val customCoverPath: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(
        entity = PdfEntity::class,
        parentColumns = ["id"],
        childColumns = ["pdfId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("pdfId")]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pdfId: Long,
    val page: Int,
    val label: String = "",
    val createdAt: Long = 0
)
