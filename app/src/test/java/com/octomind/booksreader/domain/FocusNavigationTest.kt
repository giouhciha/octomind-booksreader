package com.octomind.booksreader.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusNavigationTest {
    @Test
    fun `swipe up advances exactly one punctuation block`() {
        assertEquals(1, FocusNavigation.blockDelta(-120f, thresholdPixels = 48f))
        assertEquals(1, FocusNavigation.blockDelta(-500f, thresholdPixels = 48f))
    }

    @Test
    fun `swipe down returns exactly one punctuation block`() {
        assertEquals(-1, FocusNavigation.blockDelta(120f, thresholdPixels = 48f))
    }

    @Test
    fun `small movements do not navigate`() {
        assertEquals(0, FocusNavigation.blockDelta(47f, thresholdPixels = 48f))
        assertEquals(0, FocusNavigation.blockDelta(-47f, thresholdPixels = 48f))
    }

    @Test
    fun `marker targets a comfortable position above screen midpoint`() {
        assertEquals(
            1_060f,
            FocusNavigation.targetCenterInList(
                screenHeightPixels = 3_000,
                listTopInWindowPixels = 200f,
                viewportStartPixels = 0,
                viewportEndPixels = 2_500,
            ),
        )
    }

    @Test
    fun `marker stays inside visible reading viewport`() {
        assertEquals(
            700f,
            FocusNavigation.targetCenterInList(
                screenHeightPixels = 3_000,
                listTopInWindowPixels = 200f,
                viewportStartPixels = 0,
                viewportEndPixels = 700,
            ),
        )
    }
}
