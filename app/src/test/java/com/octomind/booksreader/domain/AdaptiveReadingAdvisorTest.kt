package com.octomind.booksreader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveReadingAdvisorTest {
    @Test
    fun `three recent backward moves suggest recovery`() {
        val now = 200_000L

        assertTrue(
            AdaptiveReadingAdvisor.shouldSuggestRecovery(
                listOf(now - 90_000, now - 30_000, now - 1_000),
                now,
            ),
        )
    }

    @Test
    fun `old or insufficient backward moves do not suggest recovery`() {
        val now = 200_000L

        assertFalse(
            AdaptiveReadingAdvisor.shouldSuggestRecovery(
                listOf(now - 130_000, now - 30_000, now - 1_000),
                now,
            ),
        )
    }

    @Test
    fun `fast manual advances increase pace by at most five percent`() {
        val suggested = AdaptiveReadingAdvisor.suggestedWordsPerMinute(
            currentWordsPerMinute = 260,
            advanceRatios = List(8) { 0.65 },
            cooldownBlocks = 0,
            hasRecentBacktracking = false,
        )

        assertEquals(273, suggested)
    }

    @Test
    fun `advance adaptation waits for enough evidence`() {
        assertNull(
            AdaptiveReadingAdvisor.suggestedWordsPerMinute(
                currentWordsPerMinute = 260,
                advanceRatios = List(7) { 0.60 },
                cooldownBlocks = 0,
                hasRecentBacktracking = false,
            ),
        )
    }

    @Test
    fun `cooldown and recent backtracking block speed increases`() {
        val fastSamples = List(8) { 0.60 }

        assertNull(AdaptiveReadingAdvisor.suggestedWordsPerMinute(260, fastSamples, 1, false))
        assertNull(AdaptiveReadingAdvisor.suggestedWordsPerMinute(260, fastSamples, 0, true))
    }
}
