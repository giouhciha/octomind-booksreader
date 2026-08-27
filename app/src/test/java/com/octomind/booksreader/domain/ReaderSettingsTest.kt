package com.octomind.booksreader.domain

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderSettingsTest {
    @Test
    fun `controls start expanded before user chooses otherwise`() {
        assertTrue(ReaderSettings().readerControlsExpanded)
    }

    @Test
    fun `books without a stored narrator safely use Octi`() {
        val book = BookSummary(
            id = "book",
            title = "Libro",
            author = null,
            format = BookFormat.TXT,
            totalWords = 1,
            totalCharacters = 1,
            currentCharacterOffset = 0,
            lastOpenedAtMillis = 0,
            calibrationCompleted = true,
        )

        assertEquals(NarratorAvatar.OCTI, book.narratorAvatar)
    }
}
