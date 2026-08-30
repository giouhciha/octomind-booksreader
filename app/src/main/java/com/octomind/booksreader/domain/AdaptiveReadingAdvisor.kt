package com.octomind.booksreader.domain

import kotlin.math.roundToInt

object AdaptiveReadingAdvisor {
    const val BACKWARD_WINDOW_MILLIS = 120_000L
    const val BACKWARD_THRESHOLD = 3
    const val MIN_MANUAL_ADVANCE_MILLIS = 400L
    const val REQUIRED_ADVANCE_SAMPLES = 8
    const val ADJUSTMENT_COOLDOWN_BLOCKS = 10
    private const val FAST_READING_RATIO = 0.80
    private const val MAXIMUM_SPEED_INCREASE = 1.05

    fun recentBackwardMoves(
        timestamps: List<Long>,
        nowMillis: Long,
    ): List<Long> = timestamps.filter { nowMillis - it in 0..BACKWARD_WINDOW_MILLIS }

    fun shouldSuggestRecovery(
        timestamps: List<Long>,
        nowMillis: Long,
    ): Boolean = recentBackwardMoves(timestamps, nowMillis).size >= BACKWARD_THRESHOLD

    fun suggestedWordsPerMinute(
        currentWordsPerMinute: Int,
        advanceRatios: List<Double>,
        cooldownBlocks: Int,
        hasRecentBacktracking: Boolean,
    ): Int? {
        if (advanceRatios.size < REQUIRED_ADVANCE_SAMPLES || cooldownBlocks > 0 || hasRecentBacktracking) {
            return null
        }
        val sample = advanceRatios.takeLast(REQUIRED_ADVANCE_SAMPLES).sorted()
        val median = (sample[3] + sample[4]) / 2.0
        if (median > FAST_READING_RATIO) return null

        val observedPace = (currentWordsPerMinute / median).roundToInt()
        val maximumIncrease = (currentWordsPerMinute * MAXIMUM_SPEED_INCREASE).roundToInt()
        return observedPace
            .coerceAtMost(maximumIncrease)
            .coerceIn(100, 700)
            .takeIf { it > currentWordsPerMinute }
    }
}
