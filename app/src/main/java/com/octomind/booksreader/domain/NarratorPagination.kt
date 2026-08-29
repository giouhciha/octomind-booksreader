package com.octomind.booksreader.domain

import kotlin.math.roundToInt

object NarratorPagination {
    private const val NATURAL_BOUNDARY_THRESHOLD = 0.6f
    private val wordPattern = Regex("\\S+")
    private val naturalBoundaries = setOf('.', '?', '!', ';', ':', ',', '—', '–')

    fun paginate(text: String, fits: (String) -> Boolean): List<String> {
        val normalizedText = text.trim()
        val words = wordPattern.findAll(normalizedText).toList()
        if (words.isEmpty()) return listOf("")
        val pages = mutableListOf<String>()
        var startWord = 0
        while (startWord < words.size) {
            var low = startWord + 1
            var high = words.size
            var maximumEnd = low
            while (low <= high) {
                val middle = (low + high) / 2
                val candidate = textBetween(normalizedText, words, startWord, middle)
                if (fits(candidate)) {
                    maximumEnd = middle
                    low = middle + 1
                } else {
                    high = middle - 1
                }
            }
            val minimumNaturalEnd = startWord +
                ((maximumEnd - startWord) * NATURAL_BOUNDARY_THRESHOLD).roundToInt()
            val naturalEnd = (maximumEnd downTo minimumNaturalEnd.coerceAtLeast(startWord + 1))
                .firstOrNull { end -> words[end - 1].value.hasNaturalBoundary() }
            val pageEnd = naturalEnd ?: maximumEnd
            pages += textBetween(normalizedText, words, startWord, pageEnd)
            startWord = pageEnd
        }
        return pages
    }

    private fun textBetween(
        source: String,
        words: List<MatchResult>,
        start: Int,
        endExclusive: Int,
    ): String = source.substring(
        words[start].range.first,
        words[endExclusive - 1].range.last + 1,
    ).trim()

    private fun String.hasNaturalBoundary(): Boolean =
        trimEnd('"', '\'', '”', '’', '»', ')', ']', '}').lastOrNull() in naturalBoundaries
}
