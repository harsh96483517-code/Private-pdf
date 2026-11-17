package com.lumen.pdf.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {

    @Query("SELECT * FROM pdfs ORDER BY sortOrder ASC, title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdfs ORDER BY sortOrder ASC, title COLLATE NOCASE ASC")
    suspend fun all(): List<PdfFile>

    @Query("SELECT * FROM pdfs WHERE folderId IS :folderId ORDER BY sortOrder ASC, title COLLATE NOCASE ASC")
    fun observeInFolder(folderId: Long?): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdfs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdfs WHERE lastOpened > 0 ORDER BY lastOpened DESC")
    fun observeRecent(): Flow<List<PdfFile>>

    @Query("SELECT * FROM pdfs WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): PdfFile?

    @Query("SELECT * FROM pdfs WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<PdfFile?>

    @Query("SELECT * FROM pdfs WHERE uri = :uri LIMIT 1")
    suspend fun byUri(uri: String): PdfFile?

    @Insert
    suspend fun insert(pdf: PdfFile): Long

    @Update
    suspend fun update(pdf: PdfFile)

    @Query("DELETE FROM pdfs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE pdfs SET folderId = :folderId, sortOrder = :order WHERE id = :id")
    suspend fun setFolder(id: Long, folderId: Long?, order: Int)

    @Query("UPDATE pdfs SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE pdfs SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: Long, order: Int)
}

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Folder>>

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    suspend fun all(): List<Folder>

    @Query("SELECT * FROM folders WHERE parentId IS :parentId ORDER BY sortOrder ASC, name COLLATE NOCASE ASC")
    fun observeChildren(parentId: Long?): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): Folder?

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Folder?

    @Insert
    suspend fun insert(folder: Folder): Long

    @Update
    suspend fun update(folder: Folder)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE folders SET parentId = :parentId WHERE parentId = :oldParent")
    suspend fun reparentChildren(oldParent: Long, parentId: Long?)
}

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE pdfId = :pdfId ORDER BY page ASC")
    fun observeForPdf(pdfId: Long): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE pdfId = :pdfId AND page = :page LIMIT 1")
    suspend fun find(pdfId: Long, page: Int): Bookmark?

    @Insert
    suspend fun insert(bookmark: Bookmark): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM bookmarks WHERE pdfId IN (:pdfIds)")
    suspend fun deleteForPdfs(pdfIds: List<Long>)
}
