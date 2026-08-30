package com.octomind.booksreader.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BookPageAnchorTest {
    private val document = BookDocument(
        summary = BookSummary(
            id = "pdf-test",
            title = "PDF sintético",
            author = null,
            format = BookFormat.PDF,
            totalWords = 20,
            totalCharacters = 200,
            currentCharacterOffset = 0,
            lastOpenedAtMillis = 0,
            calibrationCompleted = true,
        ),
        text = "Texto sintético",
        chapters = listOf(BookChapter("Inicio", 0)),
        pageAnchors = listOf(
            BookPageAnchor(pageIndex = 1, startCharacterOffset = 0),
            BookPageAnchor(pageIndex = 2, startCharacterOffset = 60),
            BookPageAnchor(pageIndex = 4, startCharacterOffset = 140),
        ),
    )

    @Test
    fun `finds original page from adapted character offset`() {
        assertEquals(1, document.pageIndexFor(0))
        assertEquals(2, document.pageIndexFor(100))
        assertEquals(4, document.pageIndexFor(199))
    }

    @Test
    fun `finds nearest text anchor for an original page without text`() {
        assertEquals(0, document.characterOffsetForPage(0))
        assertEquals(60, document.characterOffsetForPage(3))
        assertEquals(140, document.characterOffsetForPage(4))
    }
}
