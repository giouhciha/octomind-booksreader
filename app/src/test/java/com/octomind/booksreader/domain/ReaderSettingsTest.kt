package com.octomind.booksreader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSettingsTest {
    @Test
    fun `controls start expanded before user chooses otherwise`() {
        assertTrue(ReaderSettings().readerControlsExpanded)
    }

    @Test
    fun `ambient audio remains optional and quiet by default`() {
        val settings = ReaderSettings()

        assertFalse(settings.ambientAudioEnabled)
        assertEquals(AmbientSoundscape.CONCENTRATION, settings.ambientSoundscape)
        assertEquals(15, settings.ambientAudioVolumePercent)
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

    @Test
    fun `Stranger narrator keeps its stable persisted name`() {
        val storedValue = NarratorAvatar.STRANGER_ILLUSTRATION.name

        assertEquals(
            NarratorAvatar.STRANGER_ILLUSTRATION,
            NarratorAvatar.valueOf(storedValue),
        )
    }

    @Test
    fun `Lila narrator keeps its stable persisted name`() {
        val storedValue = NarratorAvatar.LILA_ILLUSTRATION.name

        assertEquals(
            NarratorAvatar.LILA_ILLUSTRATION,
            NarratorAvatar.valueOf(storedValue),
        )
    }

    @Test
    fun `Achu narrator keeps its stable persisted name`() {
        val storedValue = NarratorAvatar.ACHU_ILLUSTRATION.name

        assertEquals(
            NarratorAvatar.ACHU_ILLUSTRATION,
            NarratorAvatar.valueOf(storedValue),
        )
    }

    @Test
    fun `Frank N Furter narrator keeps its stable persisted name`() {
        val storedValue = NarratorAvatar.FRANK_N_FURTER_ILLUSTRATION.name

        assertEquals(
            NarratorAvatar.FRANK_N_FURTER_ILLUSTRATION,
            NarratorAvatar.valueOf(storedValue),
        )
    }
}
