package com.octomind.booksreader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderCalibrationTest {
    @Test
    fun `estimates pace after removing linguistic pauses`() {
        val samples =
            listOf(
                CalibrationSample(elapsedMillis = 1_000, wordCount = 4, linguisticPauseMillis = 200),
                CalibrationSample(elapsedMillis = 850, wordCount = 4, linguisticPauseMillis = 50),
                CalibrationSample(elapsedMillis = 800, wordCount = 4, linguisticPauseMillis = 0),
            )

        val estimate = ReaderCalibrationCalculator.estimateWordsPerMinute(samples, 260)

        assertEquals(300, estimate)
    }

    @Test
    fun `ignores accidental taps and interruptions`() {
        assertFalse(ReaderCalibrationCalculator.accepts(100))
        assertTrue(ReaderCalibrationCalculator.accepts(1_200))
        assertFalse(ReaderCalibrationCalculator.accepts(12_000))

        val samples =
            listOf(
                CalibrationSample(elapsedMillis = 100, wordCount = 4, linguisticPauseMillis = 0),
                CalibrationSample(elapsedMillis = 12_000, wordCount = 4, linguisticPauseMillis = 0),
            )
        assertEquals(275, ReaderCalibrationCalculator.estimateWordsPerMinute(samples, 275))
    }

    @Test
    fun `median protects the profile from a valid but extreme sample`() {
        val samples =
            listOf(
                CalibrationSample(elapsedMillis = 800, wordCount = 4, linguisticPauseMillis = 0),
                CalibrationSample(elapsedMillis = 820, wordCount = 4, linguisticPauseMillis = 20),
                CalibrationSample(elapsedMillis = 250, wordCount = 4, linguisticPauseMillis = 0),
            )

        assertEquals(300, ReaderCalibrationCalculator.estimateWordsPerMinute(samples, 260))
    }
}
