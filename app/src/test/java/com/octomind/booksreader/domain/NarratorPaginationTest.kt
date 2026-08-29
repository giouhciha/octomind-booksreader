package com.octomind.booksreader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NarratorPaginationTest {
    @Test
    fun preservesShortTextOnOnePage() {
        val pages = NarratorPagination.paginate("Una oración breve y homogénea.") { true }

        assertEquals(listOf("Una oración breve y homogénea."), pages)
    }

    @Test
    fun longTextUsesNaturalBoundaryWithoutLosingWords() {
        val text = "Uno dos tres cuatro, cinco seis siete ocho nueve diez once doce."

        val pages =
            NarratorPagination.paginate(text) { candidate ->
                candidate.split(Regex("\\s+")).size <= 6
            }

        assertEquals("Uno dos tres cuatro,", pages.first())
        assertTrue(pages.all { it.split(Regex("\\s+")).size <= 6 })
        assertEquals(text, pages.joinToString(" "))
    }

    @Test
    fun oversizedSingleWordStillMakesProgress() {
        val pages = NarratorPagination.paginate("extraordinariamente largo") { false }

        assertEquals(listOf("extraordinariamente", "largo"), pages)
    }
}
