package com.octomind.booksreader.domain

import kotlin.math.roundToLong

data class ReadingParagraph(
    val index: Int,
    val text: String,
    val startCharacterOffset: Int,
    val endCharacterOffset: Int,
)

data class ReadingBlock(
    val index: Int,
    val paragraphIndex: Int,
    val startCharacterOffset: Int,
    val endCharacterOffset: Int,
    val localStartOffset: Int,
    val localEndOffset: Int,
    val text: String,
    val wordCount: Int,
    val pauseAfterMillis: Long,
)

data class ReadingPlan(
    val paragraphs: List<ReadingParagraph>,
    val blocks: List<ReadingBlock>,
) {
    fun blockIndexFor(characterOffset: Int): Int {
        if (blocks.isEmpty()) return 0
        val exactOrPrevious = blocks.binarySearch { block ->
            when {
                block.endCharacterOffset <= characterOffset -> -1
                block.startCharacterOffset > characterOffset -> 1
                else -> 0
            }
        }
        if (exactOrPrevious >= 0) return exactOrPrevious
        return (-exactOrPrevious - 1).coerceIn(0, blocks.lastIndex)
    }
}

object ReadingPlanBuilder {
    private val wordPattern = Regex("\\S+")
    private val paragraphSeparator = Regex("\\n\\s*\\n+")
    private val closingMarks = setOf('"', '\'', '”', '’', '»', ')', ']', '}')
    private val invisibleFormattingMarks = setOf(
        '\u200B',
        '\u200C',
        '\u200D',
        '\u200E',
        '\u200F',
        '\u2060',
        '\uFEFF',
    )
    private val longDashes = setOf('–', '—', '―')
    private val initialPattern = Regex("^[\\p{L}]\\.$")
    private val initialismPattern = Regex("^(?:[\\p{L}]\\.){2,}$")
    private val nonTerminalAbbreviations = setOf(
        "sr.", "sra.", "srta.", "dr.", "dra.", "ud.", "uds.", "d.", "dna.",
        "num.", "no.", "pag.", "pp.", "vol.", "cap.", "esq.", "co.", "st.",
    )

    private enum class PauseKind(val durationMillis: Long) {
        NONE(0L),
        MEDIUM(220L),
        DASH(340L),
        STRONG(420L),
    }

    private enum class StandaloneDashRole {
        LEADING,
        OPENING,
        CLOSING,
    }

    fun build(
        text: String,
        wordsPerBlock: Int,
        readingMode: ReadingMode = ReadingMode.FIXED_WORDS,
    ): ReadingPlan {
        require(wordsPerBlock in 1..8) { "El bloque debe contener entre 1 y 8 palabras" }

        val paragraphs = mutableListOf<ReadingParagraph>()
        val blocks = mutableListOf<ReadingBlock>()
        var searchFrom = 0

        paragraphSeparator.split(text)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEachIndexed { paragraphIndex, paragraphText ->
                val paragraphStart = text.indexOf(paragraphText, searchFrom).let { found ->
                    if (found >= 0) found else searchFrom
                }
                val paragraphEnd = paragraphStart + paragraphText.length
                val paragraph = ReadingParagraph(
                    index = paragraphIndex,
                    text = paragraphText,
                    startCharacterOffset = paragraphStart,
                    endCharacterOffset = paragraphEnd,
                )
                paragraphs += paragraph
                searchFrom = paragraphEnd

                val words = wordPattern.findAll(paragraphText).toList()
                val standaloneDashRoles = classifyStandaloneDashes(words)
                var wordIndex = 0
                while (wordIndex < words.size) {
                    val maximumBlockEnd = if (
                        readingMode == ReadingMode.PUNCTUATION || readingMode == ReadingMode.SENTENCE
                    ) {
                        words.size
                    } else {
                        maximumBlockEnd(
                            words = words,
                            startWord = wordIndex,
                            wordsPerBlock = wordsPerBlock,
                            standaloneDashRoles = standaloneDashRoles,
                        )
                    }
                    val blockEndWord = findNaturalBoundary(
                        words = words,
                        startWord = wordIndex,
                        maximumEndWord = maximumBlockEnd,
                        standaloneDashRoles = standaloneDashRoles,
                        readingMode = readingMode,
                    )

                    val firstWord = words[wordIndex]
                    val lastWord = words[blockEndWord - 1]
                    val localStart = firstWord.range.first
                    val localEnd = lastWord.range.last + 1
                    val isParagraphEnd = blockEndWord == words.size
                    val nextWordStartsDash = readingMode != ReadingMode.SENTENCE && !isParagraphEnd &&
                        (standaloneDashRoles[blockEndWord] == StandaloneDashRole.OPENING ||
                            (standaloneDashRoles[blockEndWord] == null &&
                                words[blockEndWord].value.startsDiscourseDash()))
                    val blockText = paragraphText.substring(localStart, localEnd)
                    val lexicalWordCount = (wordIndex until blockEndWord)
                        .count { index -> standaloneDashRoles[index] == null }

                    blocks += ReadingBlock(
                        index = blocks.size,
                        paragraphIndex = paragraphIndex,
                        startCharacterOffset = paragraphStart + localStart,
                        endCharacterOffset = paragraphStart + localEnd,
                        localStartOffset = localStart,
                        localEndOffset = localEnd,
                        text = blockText,
                        wordCount = lexicalWordCount.coerceAtLeast(1),
                        pauseAfterMillis = punctuationPause(
                            text = blockText,
                            isParagraphEnd = isParagraphEnd,
                            nextWordStartsDash = nextWordStartsDash,
                            suppressTerminalPause = isNonTerminalAbbreviation(words, blockEndWord - 1),
                        ),
                    )
                    wordIndex = blockEndWord
                }
            }

        return ReadingPlan(paragraphs = paragraphs, blocks = blocks)
    }

    private fun classifyStandaloneDashes(
        words: List<MatchResult>,
    ): Map<Int, StandaloneDashRole> {
        val roles = mutableMapOf<Int, StandaloneDashRole>()
        var nextAsideRole = StandaloneDashRole.OPENING

        words.forEachIndexed { index, word ->
            if (!word.value.isStandaloneDash()) return@forEachIndexed

            val followsSentenceEnd = words.getOrNull(index - 1)
                ?.value
                ?.terminalPauseKind() == PauseKind.STRONG
            if (index == 0 || followsSentenceEnd) {
                roles[index] = StandaloneDashRole.LEADING
            } else {
                roles[index] = nextAsideRole
                nextAsideRole = if (nextAsideRole == StandaloneDashRole.OPENING) {
                    StandaloneDashRole.CLOSING
                } else {
                    StandaloneDashRole.OPENING
                }
            }
        }
        return roles
    }

    private fun maximumBlockEnd(
        words: List<MatchResult>,
        startWord: Int,
        wordsPerBlock: Int,
        standaloneDashRoles: Map<Int, StandaloneDashRole>,
    ): Int {
        val remainingLexicalWords = (startWord until words.size)
            .count { index -> standaloneDashRoles[index] == null }
        val remainingBlockCount = (remainingLexicalWords + wordsPerBlock - 1) / wordsPerBlock
        val targetWords = (remainingLexicalWords + remainingBlockCount - 1) / remainingBlockCount
        var endWord = startWord
        var lexicalWords = 0
        while (endWord < words.size && lexicalWords < targetWords) {
            if (standaloneDashRoles[endWord] == null) lexicalWords++
            endWord++
        }

        // El guion de cierre no cuenta como palabra y debe permanecer con la aclaración.
        if (standaloneDashRoles[endWord] == StandaloneDashRole.CLOSING) endWord++
        return endWord
    }

    private fun findNaturalBoundary(
        words: List<MatchResult>,
        startWord: Int,
        maximumEndWord: Int,
        standaloneDashRoles: Map<Int, StandaloneDashRole>,
        readingMode: ReadingMode,
    ): Int {
        for (index in startWord until maximumEndWord) {
            val dashRole = standaloneDashRoles[index]
            if (readingMode != ReadingMode.SENTENCE && dashRole == StandaloneDashRole.CLOSING) {
                return index + 1
            }
            val pauseKind = words[index].value.terminalPauseKind()
            if (dashRole == null &&
                pauseKind != PauseKind.NONE &&
                (readingMode != ReadingMode.SENTENCE || pauseKind == PauseKind.STRONG) &&
                !isNonTerminalAbbreviation(words, index)
            ) {
                return index + 1
            }

            if (readingMode == ReadingMode.SENTENCE) continue
            val nextIndex = index + 1
            val nextDashRole = standaloneDashRoles[nextIndex]
            val nextWord = words.getOrNull(nextIndex)?.value
            if (nextDashRole == StandaloneDashRole.OPENING ||
                (nextDashRole == null && nextWord?.startsDiscourseDash() == true)
            ) {
                return index + 1
            }
        }
        return maximumEndWord
    }

    private fun String.terminalPauseKind(): PauseKind {
        val trimmedText = trimEnd { character ->
            character.isWhitespace() || character in invisibleFormattingMarks
        }
        if (trimmedText.takeLastWhile { character -> !character.isWhitespace() } == "-") {
            return PauseKind.DASH
        }

        val withoutClosingMarks = trimmedText
            .trimEnd { character ->
                character in closingMarks || character in invisibleFormattingMarks
            }
        val terminalCharacter = withoutClosingMarks.lastOrNull()

        if (terminalCharacter == '.' || terminalCharacter == '!' ||
            terminalCharacter == '?' || terminalCharacter == '…'
        ) {
            return PauseKind.STRONG
        }

        val beforeSoftPunctuation = withoutClosingMarks
            .trimEnd { character -> character == ',' || character == ';' || character == ':' }
            .trimEnd { character ->
                character in closingMarks || character in invisibleFormattingMarks
            }
        if (beforeSoftPunctuation.lastOrNull() in longDashes ||
            beforeSoftPunctuation.takeLastWhile { character -> !character.isWhitespace() } == "-"
        ) {
            return PauseKind.DASH
        }

        return when (terminalCharacter) {
            ',', ';', ':' -> PauseKind.MEDIUM
            in longDashes -> PauseKind.DASH
            else -> PauseKind.NONE
        }
    }

    private fun isNonTerminalAbbreviation(words: List<MatchResult>, index: Int): Boolean {
        val nextWord = words.getOrNull(index + 1)?.value ?: return false
        val token = words.getOrNull(index)?.value
            ?.trimEnd { it in closingMarks || it in invisibleFormattingMarks }
            ?: return false
        if (!token.endsWith('.')) return false

        val normalized = java.text.Normalizer.normalize(token.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        if (initialismPattern.matches(token) || normalized in nonTerminalAbbreviations) return true

        val nextSignificantCharacter = nextWord.firstOrNull { it.isLetterOrDigit() }
        return initialPattern.matches(token) && nextSignificantCharacter?.isUpperCase() == true
    }

    private fun String.isStandaloneDash(): Boolean = this == "-" ||
        (length == 1 && first() in longDashes)

    private fun String.startsDiscourseDash(): Boolean {
        val firstCharacter = firstOrNull() ?: return false
        return when (firstCharacter) {
            in longDashes -> true
            '-' -> length == 1 || getOrNull(1)?.isDigit() == false
            else -> false
        }
    }

    private fun punctuationPause(
        text: String,
        isParagraphEnd: Boolean,
        nextWordStartsDash: Boolean,
        suppressTerminalPause: Boolean,
    ): Long {
        val pauseKind = when {
            suppressTerminalPause -> PauseKind.NONE
            text.terminalPauseKind() != PauseKind.NONE -> text.terminalPauseKind()
            nextWordStartsDash -> PauseKind.DASH
            else -> PauseKind.NONE
        }
        return pauseKind.durationMillis + if (isParagraphEnd) 320L else 0L
    }
}

object PacingCalculator {
    fun durationMillis(block: ReadingBlock, wordsPerMinute: Int): Long {
        require(wordsPerMinute in 80..1200) { "La velocidad debe estar entre 80 y 1200 PPM" }
        val readingMillis = 60_000.0 * block.wordCount / wordsPerMinute
        return (readingMillis + block.pauseAfterMillis).roundToLong().coerceAtLeast(120L)
    }
}
