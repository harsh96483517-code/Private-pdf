package com.glasspdf.app

import android.app.Application
import com.glasspdf.app.data.AppDatabase
import com.glasspdf.app.data.BackupManager
import com.glasspdf.app.data.LibraryRepository
import com.glasspdf.app.data.SettingsStore
import com.glasspdf.app.data.ThumbnailManager
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> })
    val database = AppDatabase.build(app)
    val settingsStore = SettingsStore(app)
    val thumbnails = ThumbnailManager(app)
    val repository = LibraryRepository(app, database, settingsStore, thumbnails, scope)
    val backup = BackupManager(app, database, settingsStore, repository)
}

class GlassPdfApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        container = AppContainer(this)
        // Pick up anything that was imported but not analysed yet (e.g. app was closed mid-scan).
        container.repository.startAnalysis()
    }
}
