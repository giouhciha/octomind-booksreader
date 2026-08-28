package com.octomind.booksreader.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class BookSummaryTest {
    @Test
    fun `book is completed only when its persisted offset reaches the end`() {
        assertFalse(summary(currentOffset = 999, totalCharacters = 1_000).isCompleted)
        assertTrue(summary(currentOffset = 1_000, totalCharacters = 1_000).isCompleted)
        assertTrue(summary(currentOffset = 1_001, totalCharacters = 1_000).isCompleted)
        assertFalse(summary(currentOffset = 0, totalCharacters = 0).isCompleted)
    }

    @Test
    fun `new reading cycle starts without accumulated active time`() {
        assertEquals(0L, ReadingCycleStats().activeDurationMillis)
        assertEquals(0, ReadingCycleStats().wordsRead)
    }

    private fun summary(currentOffset: Int, totalCharacters: Int) = BookSummary(
        id = "synthetic-book",
        title = "Lectura sintética",
        author = null,
        format = BookFormat.TXT,
        totalWords = 100,
        totalCharacters = totalCharacters,
        currentCharacterOffset = currentOffset,
        lastOpenedAtMillis = 0,
        calibrationCompleted = false,
    )
}
