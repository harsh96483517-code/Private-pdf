package com.glasspdf.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdfs")
    fun observeAll(): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdfs")
    suspend fun getAll(): List<PdfEntity>

    @Query("SELECT * FROM pdfs WHERE id = :id")
    suspend fun get(id: Long): PdfEntity?

    @Query("SELECT * FROM pdfs WHERE id = :id")
    fun observe(id: Long): Flow<PdfEntity?>

    @Query("SELECT uri FROM pdfs")
    suspend fun allUris(): List<String>

    @Query("SELECT * FROM pdfs WHERE analyzed = 0 AND broken = 0")
    suspend fun pending(): List<PdfEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<PdfEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: PdfEntity): Long

    @Update
    suspend fun update(item: PdfEntity)

    @Query("DELETE FROM pdfs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM pdfs")
    suspend fun deleteAll()

    @Query("UPDATE pdfs SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun moveTo(ids: List<Long>, folderId: Long?)

    @Query("UPDATE pdfs SET folderId = :newParent WHERE folderId = :folderId")
    suspend fun reparentPdfs(folderId: Long, newParent: Long?)

    @Query("UPDATE pdfs SET isFavorite = :fav WHERE id IN (:ids)")
    suspend fun setFavorite(ids: List<Long>, fav: Boolean)

    @Query("UPDATE pdfs SET customTitle = :title WHERE id = :id")
    suspend fun setTitle(id: Long, title: String?)

    @Query("UPDATE pdfs SET customCoverPath = :path WHERE id = :id")
    suspend fun setCover(id: Long, path: String?)

    @Query("UPDATE pdfs SET lastOpenedAt = :time WHERE id = :id")
    suspend fun setOpened(id: Long, time: Long)

    @Query("UPDATE pdfs SET lastOpenedAt = 0")
    suspend fun clearHistory()

    @Query("UPDATE pdfs SET lastPage = :page WHERE id = :id")
    suspend fun setLastPage(id: Long, page: Int)

    @Query("UPDATE pdfs SET sortOrder = :order WHERE id = :id")
    suspend fun setOrder(id: Long, order: Int)

    @Query("UPDATE pdfs SET uri = :uri, fileName = :name WHERE id = :id")
    suspend fun setLocation(id: Long, uri: String, name: String)

    @Query("UPDATE pdfs SET pageCount = :pages, metaTitle = :meta, analyzed = 1, broken = :broken WHERE id = :id")
    suspend fun setAnalysis(id: Long, pages: Int, meta: String?, broken: Boolean)

    @Query("UPDATE pdfs SET analyzed = 0 WHERE broken = 0")
    suspend fun markAllUnanalyzed()
}

@Dao
interface TextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PdfTextEntity)

    @Query("SELECT * FROM pdf_text WHERE pdfId IN (:ids)")
    suspend fun getFor(ids: List<Long>): List<PdfTextEntity>

    @Query("SELECT * FROM pdf_text WHERE text LIKE '%' || :q || '%' LIMIT 100")
    suspend fun search(q: String): List<PdfTextEntity>
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders")
    suspend fun getAll(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun get(id: Long): FolderEntity?

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE folders SET parentId = :parentId WHERE id = :id")
    suspend fun setParent(id: Long, parentId: Long?)

    @Query("UPDATE folders SET customCoverPath = :path WHERE id = :id")
    suspend fun setCover(id: Long, path: String?)

    @Query("UPDATE folders SET parentId = :newParent WHERE parentId = :folderId")
    suspend fun reparentChildren(folderId: Long, newParent: Long?)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM folders")
    suspend fun deleteAll()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE pdfId = :pdfId ORDER BY page")
    fun observeFor(pdfId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<BookmarkEntity>

    @Insert
    suspend fun insert(item: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM bookmarks WHERE pdfId = :pdfId AND page = :page")
    suspend fun deleteAt(pdfId: Long, page: Int)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}
