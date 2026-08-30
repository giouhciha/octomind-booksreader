package com.octomind.booksreader.audio

import com.octomind.booksreader.domain.AmbientSoundscape
import kotlin.math.abs
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientWaveformGeneratorTest {
    @Test
    fun `each soundscape produces audible bounded samples`() {
        AmbientSoundscape.entries.forEach { soundscape ->
            val samples = ShortArray(SAMPLE_COUNT)

            AmbientWaveformGenerator().fill(samples, soundscape, TEST_VOLUME_PERCENT)

            assertTrue(samples.any { it.toInt() != 0 })
            assertTrue(samples.maxOf { abs(it.toInt()) } < Short.MAX_VALUE.toInt())
        }
    }

    @Test
    fun `zero volume produces silence`() {
        val samples = ShortArray(SAMPLE_COUNT)

        AmbientWaveformGenerator().fill(
            samples,
            AmbientSoundscape.CONCENTRATION,
            volumePercent = 0,
        )

        assertTrue(samples.all { it.toInt() == 0 })
    }

    @Test
    fun `noise soundscapes are deterministic for the same seed`() {
        val first = ShortArray(SAMPLE_COUNT)
        val second = ShortArray(SAMPLE_COUNT)

        AmbientWaveformGenerator(TEST_SEED).fill(first, AmbientSoundscape.RAIN, TEST_VOLUME_PERCENT)
        AmbientWaveformGenerator(TEST_SEED).fill(second, AmbientSoundscape.RAIN, TEST_VOLUME_PERCENT)

        assertArrayEquals(first, second)
    }

    private companion object {
        const val SAMPLE_COUNT = 4_096
        const val TEST_VOLUME_PERCENT = 15
        const val TEST_SEED = 42L
    }
}
