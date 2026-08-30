package com.octomind.booksreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.octomind.booksreader.domain.PageTheme

private val OctomindLightColors =
    lightColorScheme(
        primary = Color(0xFF256D4B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD8F4E5),
        onPrimaryContainer = Color(0xFF0B3826),
        secondary = Color(0xFF4F6358),
        background = Color(0xFFF8FAF8),
        surface = Color(0xFFF8FAF8),
        surfaceVariant = Color(0xFFE7ECE8),
    )

private val OctomindDarkColors =
    darkColorScheme(
        primary = Color(0xFF84D5AA),
        onPrimary = Color(0xFF083821),
        primaryContainer = Color(0xFF174F35),
        onPrimaryContainer = Color(0xFFD8F4E5),
        secondary = Color(0xFFB7CCBF),
        background = Color(0xFF101512),
        surface = Color(0xFF101512),
        surfaceVariant = Color(0xFF29312C),
    )

private val ReaderLightColors =
    lightColorScheme(
        primary = Color(0xFF256D4B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD8F4E5),
        onPrimaryContainer = Color(0xFF0B3826),
        background = Color(0xFFFFFDF8),
        onBackground = Color(0xFF24231F),
        surface = Color(0xFFFFFDF8),
        onSurface = Color(0xFF24231F),
        surfaceVariant = Color(0xFFF0EEE7),
        onSurfaceVariant = Color(0xFF57554E),
    )

private val ReaderSepiaColors =
    lightColorScheme(
        primary = Color(0xFF725A2C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE7D4A6),
        onPrimaryContainer = Color(0xFF3C2E12),
        background = Color(0xFFF4ECD8),
        onBackground = Color(0xFF3A3024),
        surface = Color(0xFFF4ECD8),
        onSurface = Color(0xFF3A3024),
        surfaceVariant = Color(0xFFE7DDC4),
        onSurfaceVariant = Color(0xFF665A47),
    )

private val ReaderDarkColors =
    darkColorScheme(
        primary = Color(0xFF8ED5AE),
        onPrimary = Color(0xFF073821),
        primaryContainer = Color(0xFF285943),
        onPrimaryContainer = Color(0xFFE6F7ED),
        background = Color(0xFF121412),
        onBackground = Color(0xFFE3E7E3),
        surface = Color(0xFF121412),
        onSurface = Color(0xFFE3E7E3),
        surfaceVariant = Color(0xFF292D2A),
        onSurfaceVariant = Color(0xFFBCC5BE),
    )

@Composable
fun OctomindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) OctomindDarkColors else OctomindLightColors,
        typography = OctomindTypography,
        content = content,
    )
}

@Composable
fun ReaderPageTheme(
    pageTheme: PageTheme,
    content: @Composable () -> Unit,
) {
    val colors =
        when (pageTheme) {
            PageTheme.LIGHT -> ReaderLightColors
            PageTheme.SEPIA -> ReaderSepiaColors
            PageTheme.DARK -> ReaderDarkColors
        }
    MaterialTheme(colorScheme = colors, typography = OctomindTypography, content = content)
}
