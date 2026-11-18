package com.glasspdf.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PdfEntity::class, PdfTextEntity::class, FolderEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pdfDao(): PdfDao
    abstract fun textDao(): TextDao
    abstract fun folderDao(): FolderDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "glasspdf.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
