package com.octomind.booksreader

import android.app.Application
import com.octomind.booksreader.data.LocalBookRepository
import com.octomind.booksreader.data.CustomAvatarRepository
import com.octomind.booksreader.data.UserPreferences

class OctomindApplication : Application() {
    val bookRepository by lazy { LocalBookRepository(this) }
    val customAvatarRepository by lazy { CustomAvatarRepository(this) }
    val userPreferences by lazy { UserPreferences(this) }
}
