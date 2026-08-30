package com.octomind.booksreader.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingAmbienceSelectorTest {
    @Test
    fun `title and chapter have more weight than sample`() {
        val result =
            ReadingAmbienceSelector.select(
                title = "El misterio de la casa oscura",
                chapterTitle = "Una sombra en la noche",
                chapterSample = "El jardín tenía árboles y flores junto al río.",
            )

        assertEquals(ReadingAmbience.MYSTERY, result)
    }

    @Test
    fun `normalizes accents when classifying locally`() {
        val result =
            ReadingAmbienceSelector.select(
                title = "Viaje",
                chapterTitle = null,
                chapterSample = "La nave dejó la órbita del planeta y atravesó la galaxia.",
            )

        assertEquals(ReadingAmbience.SCIENCE_FICTION, result)
    }

    @Test
    fun `uses neutral ambience without recognizable evidence`() {
        val result =
            ReadingAmbienceSelector.select(
                title = "Cuaderno",
                chapterTitle = "Primera parte",
                chapterSample = "Elena abrió la puerta y comenzó a caminar.",
            )

        assertEquals(ReadingAmbience.NEUTRAL, result)
    }

    @Test
    fun `does not match keyword inside another word`() {
        val result =
            ReadingAmbienceSelector.select(
                title = "Amorfo",
                chapterTitle = null,
                chapterSample = "Una forma distinta apareció en el papel.",
            )

        assertEquals(ReadingAmbience.NEUTRAL, result)
    }
}
