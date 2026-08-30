package com.octomind.booksreader.audio

import com.octomind.booksreader.domain.AmbientSoundscape
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

internal class AmbientWaveformGenerator(
    seed: Long = DEFAULT_RANDOM_SEED,
) {
    private val random = Random(seed)
    private var sampleIndex = 0L
    private var rainLowPass = 0f
    private var brownNoise = 0f

    fun fill(
        buffer: ShortArray,
        soundscape: AmbientSoundscape,
        volumePercent: Int,
    ) {
        val volume =
            volumePercent.coerceIn(MINIMUM_VOLUME_PERCENT, MAXIMUM_VOLUME_PERCENT) /
                PERCENT_DIVISOR
        buffer.indices.forEach { index ->
            val rawSample =
                when (soundscape) {
                    AmbientSoundscape.CONCENTRATION -> concentrationSample()
                    AmbientSoundscape.RAIN -> rainSample()
                    AmbientSoundscape.BROWN_NOISE -> brownNoiseSample()
                    AmbientSoundscape.QUIET_NIGHT -> quietNightSample()
                }
            val fadeIn = (sampleIndex.toDouble() / FADE_IN_SAMPLES).coerceIn(0.0, 1.0)
            val sample =
                (rawSample * volume * MAXIMUM_SIGNAL_GAIN * fadeIn)
                    .coerceIn(MINIMUM_SAMPLE, MAXIMUM_SAMPLE)
            buffer[index] = (sample * Short.MAX_VALUE).toInt().toShort()
            sampleIndex += 1
        }
    }

    private fun concentrationSample(): Double {
        val time = sampleIndex.toDouble() / AUDIO_SAMPLE_RATE
        val breathing =
            BASE_BREATHING_LEVEL +
                BREATHING_DEPTH * sin(TWO_PI * BREATHING_FREQUENCY * time)
        val chord =
            sin(TWO_PI * ROOT_FREQUENCY * time) +
                FIRST_HARMONIC_GAIN * sin(TWO_PI * FIRST_HARMONIC_FREQUENCY * time) +
                SECOND_HARMONIC_GAIN * sin(TWO_PI * SECOND_HARMONIC_FREQUENCY * time)
        return chord * CHORD_GAIN * breathing
    }

    private fun rainSample(): Double {
        val whiteNoise = nextWhiteNoise()
        rainLowPass = RAIN_FILTER_MEMORY * rainLowPass + RAIN_FILTER_INPUT * whiteNoise
        return (
            (whiteNoise - rainLowPass) * RAIN_HIGH_FREQUENCY_GAIN +
                rainLowPass * RAIN_LOW_FREQUENCY_GAIN
        ).toDouble()
    }

    private fun quietNightSample(): Double {
        val time = sampleIndex.toDouble() / AUDIO_SAMPLE_RATE
        val slowDrift =
            NIGHT_BASE_LEVEL +
                NIGHT_DRIFT_DEPTH * sin(TWO_PI * NIGHT_DRIFT_FREQUENCY * time)
        val darkChord =
            sin(TWO_PI * NIGHT_ROOT_FREQUENCY * time) +
                NIGHT_MINOR_THIRD_GAIN * sin(TWO_PI * NIGHT_MINOR_THIRD_FREQUENCY * time) +
                NIGHT_FIFTH_GAIN * sin(TWO_PI * NIGHT_FIFTH_FREQUENCY * time)
        return darkChord * NIGHT_CHORD_GAIN * slowDrift
    }

    private fun brownNoiseSample(): Double {
        brownNoise =
            ((brownNoise + nextWhiteNoise() * BROWN_NOISE_STEP) * BROWN_NOISE_DECAY)
                .coerceIn(MINIMUM_SAMPLE.toFloat(), MAXIMUM_SAMPLE.toFloat())
        return (brownNoise * BROWN_NOISE_GAIN).toDouble()
    }

    private fun nextWhiteNoise(): Float = random.nextFloat() * TWO - ONE

    private companion object {
        const val DEFAULT_RANDOM_SEED = 8_021L
        const val AUDIO_SAMPLE_RATE = 22_050.0
        const val FADE_IN_SAMPLES = AUDIO_SAMPLE_RATE
        const val PERCENT_DIVISOR = 100.0
        const val MAXIMUM_SIGNAL_GAIN = 0.65
        const val MINIMUM_VOLUME_PERCENT = 0
        const val MAXIMUM_VOLUME_PERCENT = 50
        const val MINIMUM_SAMPLE = -1.0
        const val MAXIMUM_SAMPLE = 1.0
        const val TWO = 2f
        const val ONE = 1f
        const val TWO_PI = 2.0 * PI
        const val BASE_BREATHING_LEVEL = 0.88
        const val BREATHING_DEPTH = 0.12
        const val BREATHING_FREQUENCY = 0.045
        const val ROOT_FREQUENCY = 110.0
        const val FIRST_HARMONIC_FREQUENCY = 164.81
        const val SECOND_HARMONIC_FREQUENCY = 220.0
        const val FIRST_HARMONIC_GAIN = 0.55
        const val SECOND_HARMONIC_GAIN = 0.28
        const val CHORD_GAIN = 0.28
        const val RAIN_FILTER_MEMORY = 0.96f
        const val RAIN_FILTER_INPUT = 0.04f
        const val RAIN_HIGH_FREQUENCY_GAIN = 0.34f
        const val RAIN_LOW_FREQUENCY_GAIN = 0.14f
        const val BROWN_NOISE_STEP = 0.025f
        const val BROWN_NOISE_DECAY = 0.996f
        const val BROWN_NOISE_GAIN = 0.9f
        const val NIGHT_BASE_LEVEL = 0.86
        const val NIGHT_DRIFT_DEPTH = 0.14
        const val NIGHT_DRIFT_FREQUENCY = 0.025
        const val NIGHT_ROOT_FREQUENCY = 73.42
        const val NIGHT_MINOR_THIRD_FREQUENCY = 87.31
        const val NIGHT_FIFTH_FREQUENCY = 110.0
        const val NIGHT_MINOR_THIRD_GAIN = 0.62
        const val NIGHT_FIFTH_GAIN = 0.36
        const val NIGHT_CHORD_GAIN = 0.25
    }
}
