package com.octomind.booksreader

import android.app.Application
import com.octomind.booksreader.data.BackupRepository
import com.octomind.booksreader.data.CustomAvatarRepository
import com.octomind.booksreader.data.LocalBookRepository
import com.octomind.booksreader.data.LocalComprehensionRepository
import com.octomind.booksreader.data.UserPreferences
import com.octomind.booksreader.domain.LocalRecallQuestionProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class OctomindApplication : Application() {
    val bookRepository by lazy { LocalBookRepository(this) }
    val customAvatarRepository by lazy { CustomAvatarRepository(this) }
    val comprehensionRepository by lazy { LocalComprehensionRepository(this) }
    val comprehensionQuestionProvider by lazy { LocalRecallQuestionProvider() }
    val userPreferences by lazy { UserPreferences(this) }
    val backupRepository by lazy { BackupRepository(this, userPreferences) }

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
    }
}
