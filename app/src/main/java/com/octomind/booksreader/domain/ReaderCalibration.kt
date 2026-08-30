package com.octomind.booksreader.domain

import kotlin.math.roundToInt

data class CalibrationSample(
    val elapsedMillis: Long,
    val wordCount: Int,
    val linguisticPauseMillis: Long,
)

data class ReaderProfile(
    val baselineWordsPerMinute: Int = 260,
    val calibrationSampleCount: Int = 0,
    val completedCalibrations: Int = 0,
)

object ReaderCalibrationCalculator {
    const val TARGET_SAMPLE_COUNT = 30
    const val MIN_TAP_INTERVAL_MILLIS = 150L
    const val MAX_TAP_INTERVAL_MILLIS = 10_000L
    private const val MINIMUM_ESTIMATE_WPM = 80.0
    private const val MAXIMUM_ESTIMATE_WPM = 1_200.0
    private const val MINIMUM_RESULT_WPM = 100
    private const val MAXIMUM_RESULT_WPM = 700

    fun accepts(elapsedMillis: Long): Boolean = elapsedMillis in MIN_TAP_INTERVAL_MILLIS..MAX_TAP_INTERVAL_MILLIS

    fun estimateWordsPerMinute(
        samples: List<CalibrationSample>,
        fallbackWordsPerMinute: Int,
    ): Int {
        val estimates =
            samples
                .mapNotNull { sample ->
                    if (!accepts(sample.elapsedMillis) || sample.wordCount <= 0) return@mapNotNull null
                    val readingMillis =
                        (sample.elapsedMillis - sample.linguisticPauseMillis)
                            .coerceAtLeast(MIN_TAP_INTERVAL_MILLIS)
                    val estimate = 60_000.0 * sample.wordCount / readingMillis
                    estimate.takeIf { it in MINIMUM_ESTIMATE_WPM..MAXIMUM_ESTIMATE_WPM }
                }.sorted()

        if (estimates.isEmpty()) return fallbackWordsPerMinute.coerceIn(MINIMUM_RESULT_WPM, MAXIMUM_RESULT_WPM)
        val middle = estimates.size / 2
        val median =
            if (estimates.size % 2 == 0) {
                (estimates[middle - 1] + estimates[middle]) / 2.0
            } else {
                estimates[middle]
            }
        return median.roundToInt().coerceIn(MINIMUM_RESULT_WPM, MAXIMUM_RESULT_WPM)
    }
}
