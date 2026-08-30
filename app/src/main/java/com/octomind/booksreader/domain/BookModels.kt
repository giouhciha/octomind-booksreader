package com.octomind.booksreader.domain

enum class BookFormat {
    TXT,
    EPUB,
    PDF,
}

data class BookSummary(
    val id: String,
    val title: String,
    val author: String?,
    val format: BookFormat,
    val totalWords: Int,
    val totalCharacters: Int,
    val currentCharacterOffset: Int,
    val lastOpenedAtMillis: Long,
    val calibrationCompleted: Boolean,
    val coverImagePath: String? = null,
    val narratorAvatar: NarratorAvatar = NarratorAvatar.OCTI,
    val completedReadings: List<CompletedReading> = emptyList(),
    val currentCycleStats: ReadingCycleStats = ReadingCycleStats(),
    val savedQuotes: List<SavedQuote> = emptyList(),
) {
    val progress: Float
        get() =
            if (totalCharacters == 0) {
                0f
            } else {
                (currentCharacterOffset.toFloat() / totalCharacters).coerceIn(0f, 1f)
            }

    val isCompleted: Boolean
        get() = totalCharacters > 0 && currentCharacterOffset >= totalCharacters
}

data class SavedQuote(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val chapterTitle: String?,
    val text: String,
    val startCharacterOffset: Int,
    val endCharacterOffset: Int,
    val createdAtMillis: Long,
)

data class CompletedReading(
    val completedAtMillis: Long,
    val elapsedMillis: Long,
    val wordsRead: Int,
    val averageWordsPerMinute: Int,
    val pauses: Int,
    val backwardsMoves: Int,
    val fragmentsRead: Int,
)

data class ReadingCycleStats(
    val activeDurationMillis: Long = 0,
    val wordsRead: Int = 0,
    val pauses: Int = 0,
    val backwardsMoves: Int = 0,
    val fragmentsRead: Int = 0,
)

data class BookChapter(
    val title: String,
    val startCharacterOffset: Int,
)

data class BookPageAnchor(
    val pageIndex: Int,
    val startCharacterOffset: Int,
)

data class BookDocument(
    val summary: BookSummary,
    val text: String,
    val chapters: List<BookChapter>,
    val originalFilePath: String? = null,
    val pageAnchors: List<BookPageAnchor> = emptyList(),
) {
    val exactProgress: Float
        get() = summary.progress

    fun pageIndexFor(characterOffset: Int): Int? =
        pageAnchors
            .lastOrNull { it.startCharacterOffset <= characterOffset }
            ?.pageIndex
            ?: pageAnchors.firstOrNull()?.pageIndex

    fun characterOffsetForPage(pageIndex: Int): Int? =
        pageAnchors
            .lastOrNull { it.pageIndex <= pageIndex }
            ?.startCharacterOffset
            ?: pageAnchors.firstOrNull()?.startCharacterOffset
}

data class ParsedBook(
    val title: String,
    val author: String?,
    val format: BookFormat,
    val text: String,
    val chapters: List<BookChapter>,
    val coverImage: ByteArray? = null,
    val pageAnchors: List<BookPageAnchor> = emptyList(),
)

enum class PageTheme {
    LIGHT,
    SEPIA,
    DARK,
}

enum class ReadingMode {
    FIXED_WORDS,
    PUNCTUATION,
    SENTENCE,
}

enum class ReaderFontStyle {
    SERIF,
    SANS_SERIF,
    MONOSPACE,
}

enum class FocusPresentation {
    TEXT_MARKER,
    OCTI_NARRATOR,
}

enum class NarratorAvatar {
    OCTI,
    LOVECRAFT_ILLUSTRATION,
    SCHOPENHAUER_ILLUSTRATION,
    NIETZSCHE_ILLUSTRATION,
    CAMUS_ILLUSTRATION,
    STRANGER_ILLUSTRATION,
    LILA_ILLUSTRATION,
    ACHU_ILLUSTRATION,
    FRANK_N_FURTER_ILLUSTRATION,
    CUSTOM_IMAGE,
}

object CustomAvatarPolicy {
    const val MAX_FILE_BYTES: Long = 10L * 1024L * 1024L
    const val MIN_EDGE_PIXELS: Int = 128
    const val MAX_EDGE_PIXELS: Int = 8_192
    const val MAX_PIXEL_COUNT: Long = 40_000_000L

    private val allowedMimeTypes = setOf("image/jpeg", "image/png", "image/webp")

    fun accepts(
        mimeType: String?,
        fileBytes: Long?,
        width: Int,
        height: Int,
    ): Boolean =
        mimeType in allowedMimeTypes &&
            (fileBytes == null || fileBytes in 1..MAX_FILE_BYTES) &&
            width in MIN_EDGE_PIXELS..MAX_EDGE_PIXELS &&
            height in MIN_EDGE_PIXELS..MAX_EDGE_PIXELS &&
            width.toLong() * height.toLong() <= MAX_PIXEL_COUNT
}

enum class AmbientIntensity {
    OFF,
    SUBTLE,
    IMMERSIVE,
}

enum class AmbientSoundscape {
    CONCENTRATION,
    RAIN,
    BROWN_NOISE,
    QUIET_NIGHT,
}

data class ReaderSettings(
    val wordsPerMinute: Int = 260,
    val wordsPerBlock: Int = 4,
    val readingMode: ReadingMode = ReadingMode.FIXED_WORDS,
    val pageTheme: PageTheme = PageTheme.LIGHT,
    val fontStyle: ReaderFontStyle = ReaderFontStyle.SERIF,
    val fontSizeSp: Int = 19,
    val adaptivePacingEnabled: Boolean = true,
    val focusDimmingPercent: Int = 45,
    val showFocusMascot: Boolean = true,
    val focusPresentation: FocusPresentation = FocusPresentation.OCTI_NARRATOR,
    val narratorAvatar: NarratorAvatar = NarratorAvatar.OCTI,
    val customNarratorAvatarVersion: Int = 0,
    val ambientIntensity: AmbientIntensity = AmbientIntensity.SUBTLE,
    val ambientAudioEnabled: Boolean = false,
    val ambientSoundscape: AmbientSoundscape = AmbientSoundscape.CONCENTRATION,
    val ambientAudioVolumePercent: Int = 15,
    val focusEnabled: Boolean = false,
    val readerControlsExpanded: Boolean = true,
    val narratorGestureHintDismissed: Boolean = false,
)

data class ReadingSessionSummary(
    val bookTitle: String,
    val coverImagePath: String? = null,
    val elapsedMillis: Long,
    val wordsRead: Int,
    val averageWordsPerMinute: Int,
    val progress: Float,
    val pauses: Int,
    val backwardsMoves: Int,
    val fragmentsRead: Int,
)
