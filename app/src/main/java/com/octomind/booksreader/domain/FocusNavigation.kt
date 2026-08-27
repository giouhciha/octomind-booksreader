package com.octomind.booksreader.domain

object FocusNavigation {
    const val READING_ANCHOR_FRACTION = 0.42f

    fun blockDelta(totalVerticalDragPixels: Float, thresholdPixels: Float): Int {
        require(thresholdPixels > 0f) { "El umbral del gesto debe ser positivo" }
        return when {
            totalVerticalDragPixels <= -thresholdPixels -> 1
            totalVerticalDragPixels >= thresholdPixels -> -1
            else -> 0
        }
    }

    fun targetCenterInList(
        screenHeightPixels: Int,
        listTopInWindowPixels: Float,
        viewportStartPixels: Int,
        viewportEndPixels: Int,
    ): Float {
        require(screenHeightPixels >= 0) { "La altura de pantalla no puede ser negativa" }
        require(viewportEndPixels >= viewportStartPixels) { "El viewport no es válido" }
        return (screenHeightPixels * READING_ANCHOR_FRACTION - listTopInWindowPixels).coerceIn(
            viewportStartPixels.toFloat(),
            viewportEndPixels.toFloat(),
        )
    }
}
