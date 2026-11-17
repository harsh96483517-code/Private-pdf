package com.lumen.pdf

import android.app.Application
import com.lumen.pdf.data.AppDatabase
import com.lumen.pdf.data.LibraryRepository
import com.lumen.pdf.data.SettingsStore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class LumenApp : Application() {

    lateinit var repo: LibraryRepository
        private set

    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        settings = SettingsStore(this)
        repo = LibraryRepository(this, AppDatabase.get(this))
    }
}
