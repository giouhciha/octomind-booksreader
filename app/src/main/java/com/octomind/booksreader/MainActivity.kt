package com.octomind.booksreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.octomind.booksreader.ui.OctomindApp
import com.octomind.booksreader.ui.theme.OctomindTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OctomindTheme {
                OctomindApp()
            }
        }
    }
}
