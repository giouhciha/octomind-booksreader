package com.octomind.booksreader.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.octomind.booksreader.OctomindApplication
import com.octomind.booksreader.domain.AmbientSoundscape
import com.octomind.booksreader.domain.BookDocument
import com.octomind.booksreader.domain.BookSummary
import com.octomind.booksreader.domain.CompletedReading
import com.octomind.booksreader.domain.NarratorAvatar
import com.octomind.booksreader.domain.PageTheme
import com.octomind.booksreader.domain.ReaderFontStyle
import com.octomind.booksreader.domain.ReaderSettings
import com.octomind.booksreader.domain.ReadingCycleStats
import com.octomind.booksreader.domain.ReadingMode
import com.octomind.booksreader.domain.ReadingPlan
import com.octomind.booksreader.domain.ReadingPlanBuilder
import com.octomind.booksreader.domain.ReadingSessionSummary
import com.octomind.booksreader.domain.SavedQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

sealed interface AppScreen {
    data object Starting : AppScreen

    data object Onboarding : AppScreen

    data object Library : AppScreen

    data object Quotes : AppScreen

    data class Reader(
        val state: ReaderState,
    ) : AppScreen

    data class SessionResult(
        val bookId: String,
        val summary: ReadingSessionSummary,
        val previousReadings: List<CompletedReading> = emptyList(),
        val restartAvailable: Boolean = false,
    ) : AppScreen
}

data class ReaderState(
    val document: BookDocument,
    val plan: ReadingPlan,
    val settings: ReaderSettings,
    val customNarratorAvatarPath: String? = null,
    val currentBlockIndex: Int,
    val focusEnabled: Boolean = false,
    val sessionStartBlockIndex: Int = currentBlockIndex,
    val furthestBlockIndex: Int = currentBlockIndex,
    val activeDurationMillis: Long = 0,
    val playStartedAtMillis: Long? = null,
    val pauses: Int = 0,
    val backwardsMoves: Int = 0,
    val completed: Boolean = false,
    val blockShownAtMillis: Long = 0L,
    val lastManualNavigationAtMillis: Long = 0L,
    val recentBackwardMoveTimestamps: List<Long> = emptyList(),
    val manualAdvanceRatios: List<Double> = emptyList(),
    val adaptationCooldownBlocks: Int = 0,
    val quotePreview: Boolean = false,
    val previewQuoteStartOffset: Int? = null,
    val previewQuoteEndOffset: Int? = null,
    val originalPdfVisible: Boolean = false,
    val originalPdfPageIndex: Int = 0,
) {
    val currentCharacterOffset: Int
        get() =
            if (completed) {
                document.text.length
            } else {
                plan.blocks.getOrNull(currentBlockIndex)?.startCharacterOffset ?: 0
            }

    val progress: Float
        get() =
            if (document.text.isEmpty()) {
                0f
            } else {
                (currentCharacterOffset.toFloat() / document.text.length).coerceIn(0f, 1f)
            }
}

data class OctomindUiState(
    val screen: AppScreen = AppScreen.Starting,
    val books: List<BookSummary> = emptyList(),
    val quotes: List<SavedQuote> = emptyList(),
    val customNarratorAvatarPath: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

class OctomindViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val app = application as OctomindApplication
    private val repository = app.bookRepository
    private val preferences = app.userPreferences
    private val mutableState = MutableStateFlow(OctomindUiState())
    val state: StateFlow<OctomindUiState> = mutableState.asStateFlow()

    private var progressSaveJob: Job? = null

    init {
        viewModelScope.launch {
            val confirmed = preferences.adultConfirmed.first()
            if (confirmed) {
                reloadLibrary()
                mutableState.update { it.copy(screen = AppScreen.Library) }
            } else {
                mutableState.update { it.copy(screen = AppScreen.Onboarding) }
            }
        }
    }

    fun confirmAdult() {
        viewModelScope.launch {
            preferences.confirmAdult()
            reloadLibrary()
            mutableState.update { it.copy(screen = AppScreen.Library) }
        }
    }

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null) }
            runCatching { repository.importBook(uri) }
                .onSuccess { summary ->
                    reloadLibrary()
                    openBook(summary.id)
                }.onFailure { error ->
                    mutableState.update {
                        it.copy(busy = false, message = error.userMessage("No fue posible importar el libro"))
                    }
                }
        }
    }

    fun openBook(id: String) {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null) }
            runCatching {
                val document = repository.loadBook(id)
                if (document.summary.isCompleted) {
                    return@runCatching completedBookResult(document.summary)
                }
                val settings =
                    preferences.readerSettings.first().copy(
                        readingMode = ReadingMode.SENTENCE,
                        adaptivePacingEnabled = false,
                    )
                val plan =
                    withContext(Dispatchers.Default) {
                        ReadingPlanBuilder.build(
                            text = document.text,
                            wordsPerBlock = settings.wordsPerBlock,
                            readingMode = settings.readingMode,
                        )
                    }
                require(plan.blocks.isNotEmpty()) { "El libro no contiene palabras legibles" }
                val currentBlockIndex = plan.blockIndexFor(document.summary.currentCharacterOffset)
                val restoredFocusEnabled = settings.focusEnabled
                val restoredSettings =
                    settings.copy(
                        narratorAvatar = document.summary.narratorAvatar,
                        readerControlsExpanded =
                            if (restoredFocusEnabled) {
                                false
                            } else {
                                settings.readerControlsExpanded
                            },
                    )
                val now = SystemClock.elapsedRealtime()
                ReaderState(
                    document = document,
                    plan = plan,
                    settings = restoredSettings,
                    customNarratorAvatarPath =
                        app.customAvatarRepository.avatarFile
                            .takeIf { it.isFile }
                            ?.absolutePath,
                    currentBlockIndex = currentBlockIndex,
                    focusEnabled = restoredFocusEnabled,
                    completed = document.summary.currentCharacterOffset >= document.text.length,
                    blockShownAtMillis = now,
                    playStartedAtMillis = now.takeIf { restoredFocusEnabled },
                )
            }.onSuccess { destination ->
                val screen =
                    when (destination) {
                        is ReaderState -> AppScreen.Reader(destination)
                        is AppScreen.SessionResult -> destination
                        else -> error("Destino de lectura no compatible")
                    }
                mutableState.update { it.copy(screen = screen, busy = false) }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(busy = false, message = error.userMessage("No fue posible abrir el libro"))
                }
            }
        }
    }

    fun deleteBook(id: String) {
        viewModelScope.launch {
            repository.deleteBook(id)
            reloadLibrary()
        }
    }

    fun createBackup(
        uri: Uri,
        password: CharArray,
    ) {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null) }
            runCatching { app.backupRepository.create(uri, password) }
                .onSuccess {
                    mutableState.update { it.copy(busy = false, message = "Respaldo creado correctamente") }
                }.onFailure { error ->
                    mutableState.update {
                        it.copy(busy = false, message = error.userMessage("No fue posible crear el respaldo"))
                    }
                }
        }
    }

    fun restoreBackup(
        uri: Uri,
        password: CharArray,
    ) {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null) }
            runCatching { app.backupRepository.restore(uri, password) }
                .onSuccess {
                    reloadLibrary()
                    mutableState.update {
                        it.copy(screen = AppScreen.Library, message = "Respaldo restaurado correctamente")
                    }
                }.onFailure { error ->
                    mutableState.update {
                        it.copy(busy = false, message = error.userMessage("No fue posible restaurar el respaldo"))
                    }
                }
        }
    }

    fun showQuotes() {
        viewModelScope.launch {
            val quotes = repository.listQuotes()
            mutableState.update { it.copy(screen = AppScreen.Quotes, quotes = quotes) }
        }
    }

    fun closeQuotes() {
        mutableState.update { it.copy(screen = AppScreen.Library) }
    }

    fun saveCurrentQuote() {
        val reader = currentReader() ?: return
        val block = reader.plan.blocks.getOrNull(reader.currentBlockIndex) ?: return
        saveQuoteRange(
            reader = reader,
            text = block.text,
            startCharacterOffset = block.startCharacterOffset,
            endCharacterOffset = block.endCharacterOffset,
        )
    }

    fun saveSelectedQuote(
        text: String,
        startCharacterOffset: Int,
        endCharacterOffset: Int,
    ) {
        val reader = currentReader()?.takeUnless { it.focusEnabled || it.quotePreview } ?: return
        if (text.isBlank() || startCharacterOffset >= endCharacterOffset) return
        saveQuoteRange(reader, text, startCharacterOffset, endCharacterOffset)
    }

    private fun saveQuoteRange(
        reader: ReaderState,
        text: String,
        startCharacterOffset: Int,
        endCharacterOffset: Int,
    ) {
        val chapter =
            reader.document.chapters
                .lastOrNull { it.startCharacterOffset <= startCharacterOffset }
                ?.title
        viewModelScope.launch {
            val saved =
                repository.saveQuote(
                    bookId = reader.document.summary.id,
                    chapterTitle = chapter,
                    text = text,
                    startCharacterOffset = startCharacterOffset,
                    endCharacterOffset = endCharacterOffset,
                )
            mutableState.update {
                it.copy(message = if (saved) "Cita guardada" else "Esta cita ya estaba guardada")
            }
        }
    }

    fun deleteQuote(quote: SavedQuote) {
        viewModelScope.launch {
            repository.deleteQuote(quote.bookId, quote.id)
            mutableState.update { it.copy(quotes = repository.listQuotes()) }
        }
    }

    fun openQuote(quote: SavedQuote) {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null) }
            runCatching {
                val document = repository.loadBook(quote.bookId)
                val settings =
                    preferences.readerSettings.first().copy(
                        readingMode = ReadingMode.SENTENCE,
                        adaptivePacingEnabled = false,
                        focusEnabled = false,
                        readerControlsExpanded = false,
                        narratorAvatar = document.summary.narratorAvatar,
                    )
                val plan =
                    withContext(Dispatchers.Default) {
                        ReadingPlanBuilder.build(
                            text = document.text,
                            wordsPerBlock = settings.wordsPerBlock,
                            readingMode = settings.readingMode,
                        )
                    }
                require(plan.blocks.isNotEmpty()) { "El libro no contiene palabras legibles" }
                val blockIndex = plan.blockIndexFor(quote.startCharacterOffset)
                ReaderState(
                    document = document,
                    plan = plan,
                    settings = settings,
                    customNarratorAvatarPath =
                        app.customAvatarRepository.avatarFile
                            .takeIf { it.isFile }
                            ?.absolutePath,
                    currentBlockIndex = blockIndex,
                    focusEnabled = false,
                    blockShownAtMillis = SystemClock.elapsedRealtime(),
                    quotePreview = true,
                    previewQuoteStartOffset = quote.startCharacterOffset,
                    previewQuoteEndOffset = quote.endCharacterOffset,
                )
            }.onSuccess { reader ->
                mutableState.update { it.copy(screen = AppScreen.Reader(reader), busy = false) }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(busy = false, message = error.userMessage("No fue posible abrir la cita"))
                }
            }
        }
    }

    fun toggleFocus() {
        val reader = currentReader() ?: return
        if (reader.focusEnabled) {
            pauseFocusSession()
            val settings = (currentReader() ?: reader).settings.copy(focusEnabled = false)
            updateReader { it.copy(focusEnabled = false, settings = settings) }
            saveSettings(settings)
        } else {
            val now = SystemClock.elapsedRealtime()
            val settings =
                reader.settings.copy(
                    focusEnabled = true,
                    readerControlsExpanded = false,
                )
            updateReader {
                it.copy(
                    focusEnabled = true,
                    playStartedAtMillis = now,
                    blockShownAtMillis = now,
                    settings = settings,
                )
            }
            saveSettings(settings)
        }
    }

    fun dismissNarratorGestureHint() {
        val reader = currentReader() ?: return
        if (reader.settings.narratorGestureHintDismissed) return
        val settings = reader.settings.copy(narratorGestureHintDismissed = true)
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
    }

    fun pauseForBackground() {
        if (currentReader()?.focusEnabled == true) pauseFocusSession()
        persistProgressImmediately()
        persistReadingCycleStats()
    }

    fun resumeForForeground() {
        updateReader { reader ->
            if (reader.focusEnabled && reader.playStartedAtMillis == null) {
                reader.copy(playStartedAtMillis = SystemClock.elapsedRealtime())
            } else {
                reader
            }
        }
    }

    fun moveBlock(delta: Int) {
        val reader = currentReader() ?: return
        if (reader.plan.blocks.isEmpty()) return
        if (delta > 0 && reader.currentBlockIndex == reader.plan.blocks.lastIndex) {
            completeReading(reader)
            return
        }
        val next = (reader.currentBlockIndex + delta).coerceIn(0, reader.plan.blocks.lastIndex)
        if (next == reader.currentBlockIndex) return

        moveToBlock(next, backwards = delta < 0)
    }

    fun completeCurrentBook() {
        val reader = currentReader() ?: return
        if (!reader.focusEnabled) completeReading(reader)
    }

    fun showOriginalPdf() {
        val reader = currentReader() ?: return
        if (reader.focusEnabled || reader.document.originalFilePath == null) return
        val pageIndex = reader.document.pageIndexFor(reader.currentCharacterOffset) ?: 0
        updateReader { it.copy(originalPdfVisible = true, originalPdfPageIndex = pageIndex) }
    }

    fun hideOriginalPdf() {
        updateReader { it.copy(originalPdfVisible = false) }
    }

    fun setOriginalPdfPage(pageIndex: Int) {
        val reader = currentReader()?.takeIf { it.originalPdfVisible } ?: return
        val safePageIndex = pageIndex.coerceAtLeast(0)
        reader.document.characterOffsetForPage(safePageIndex)?.let { characterOffset ->
            val blockIndex = reader.plan.blockIndexFor(characterOffset)
            if (blockIndex != reader.currentBlockIndex) {
                moveToBlock(blockIndex, backwards = safePageIndex < reader.originalPdfPageIndex)
            }
        }
        updateReader { it.copy(originalPdfPageIndex = safePageIndex) }
    }

    fun finishOriginalPdfReading() {
        val reader = currentReader()?.takeIf { it.originalPdfVisible } ?: return
        completeReading(reader)
        finishReader()
    }

    fun setVisibleParagraph(paragraphIndex: Int) {
        val reader = currentReader() ?: return
        if (reader.focusEnabled) return
        val block = reader.plan.blocks.firstOrNull { it.paragraphIndex == paragraphIndex } ?: return
        if (block.index == reader.currentBlockIndex) return
        moveToBlock(block.index, backwards = false)
    }

    fun updatePageTheme(pageTheme: PageTheme) {
        val reader = currentReader() ?: return
        val settings = reader.settings.copy(pageTheme = pageTheme)
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
    }

    fun updateFontStyle(fontStyle: ReaderFontStyle) {
        val reader = currentReader() ?: return
        val settings = reader.settings.copy(fontStyle = fontStyle)
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
    }

    fun updateFontSize(fontSizeSp: Int) {
        val reader = currentReader() ?: return
        val settings = reader.settings.copy(fontSizeSp = fontSizeSp.coerceIn(14, 32))
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
    }

    fun updateFocusDimming(percent: Int) {
        val reader = currentReader() ?: return
        val settings = reader.settings.copy(focusDimmingPercent = percent.coerceIn(0, 80))
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
    }

    fun updateShowFocusMascot(show: Boolean) {
        val reader = currentReader() ?: return
        val settings = reader.settings.copy(showFocusMascot = show)
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
    }

    fun updateFocusPresentation(presentation: com.octomind.booksreader.domain.FocusPresentation) {
        val reader = currentReader() ?: return
        val settings = reader.settings.copy(focusPresentation = presentation)
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
    }

    fun updateNarratorAvatar(avatar: NarratorAvatar) {
        val reader = currentReader() ?: return
        val settings = reader.settings.copy(narratorAvatar = avatar)
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
        viewModelScope.launch {
            repository.saveNarratorAvatar(reader.document.summary.id, avatar)
        }
    }

    fun importCustomNarratorAvatar(uri: Uri) {
        val reader = currentReader() ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { app.customAvatarRepository.import(uri) }
            }.onSuccess {
                val latestReader = currentReader() ?: reader
                val version = (latestReader.settings.customNarratorAvatarVersion + 1).coerceAtLeast(1)
                val settings =
                    latestReader.settings.copy(
                        narratorAvatar = NarratorAvatar.CUSTOM_IMAGE,
                        customNarratorAvatarVersion = version,
                    )
                updateReader {
                    it.copy(
                        settings = settings,
                        customNarratorAvatarPath = app.customAvatarRepository.avatarFile.absolutePath,
                    )
                }
                saveSettings(settings)
                repository.saveNarratorAvatar(
                    latestReader.document.summary.id,
                    NarratorAvatar.CUSTOM_IMAGE,
                )
                mutableState.update { it.copy(message = "Imagen personalizada guardada en este dispositivo") }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(message = error.userMessage("No fue posible importar la imagen"))
                }
            }
        }
    }

    fun deleteCustomNarratorAvatar() {
        val reader = currentReader() ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { app.customAvatarRepository.delete() }
                repository.replaceNarratorAvatarForAll(
                    NarratorAvatar.CUSTOM_IMAGE,
                    NarratorAvatar.OCTI,
                )
            }.onSuccess {
                val latestReader = currentReader() ?: reader
                val settings =
                    latestReader.settings.copy(
                        narratorAvatar = NarratorAvatar.OCTI,
                        customNarratorAvatarVersion =
                            (latestReader.settings.customNarratorAvatarVersion + 1).coerceAtLeast(1),
                    )
                updateReader { it.copy(settings = settings, customNarratorAvatarPath = null) }
                saveSettings(settings)
                mutableState.update { it.copy(message = "Imagen personalizada eliminada") }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(message = error.userMessage("No fue posible eliminar la imagen"))
                }
            }
        }
    }

    fun updateReaderControlsExpanded(expanded: Boolean) {
        val reader = currentReader() ?: return
        val settings = reader.settings.copy(readerControlsExpanded = expanded)
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
    }

    fun updateAmbientIntensity(intensity: com.octomind.booksreader.domain.AmbientIntensity) {
        val reader = currentReader() ?: return
        val settings = reader.settings.copy(ambientIntensity = intensity)
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
    }

    fun updateAmbientAudioEnabled(enabled: Boolean) {
        val reader = currentReader() ?: return
        val settings = reader.settings.copy(ambientAudioEnabled = enabled)
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
    }

    fun updateAmbientSoundscape(soundscape: AmbientSoundscape) {
        val reader = currentReader() ?: return
        val settings = reader.settings.copy(ambientSoundscape = soundscape)
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
    }

    fun updateAmbientAudioVolume(volumePercent: Int) {
        val reader = currentReader() ?: return
        val settings =
            reader.settings.copy(
                ambientAudioVolumePercent =
                    volumePercent.coerceIn(
                        MINIMUM_AMBIENT_AUDIO_VOLUME_PERCENT,
                        MAXIMUM_AMBIENT_AUDIO_VOLUME_PERCENT,
                    ),
            )
        updateReader { it.copy(settings = settings) }
        saveSettings(settings)
    }

    fun finishReader() {
        val reader = currentReader() ?: return
        if (reader.quotePreview) {
            showQuotes()
            return
        }
        pauseFocusSession()
        val finalReader = currentReader() ?: reader
        persistProgressImmediately()
        val cycleStats = finalReader.aggregateCycleStats()
        if (!finalReader.completed) {
            viewModelScope.launch {
                repository.saveReadingCycleStats(finalReader.document.summary.id, cycleStats)
            }
        }
        val average = cycleStats.averageWordsPerMinute()
        val summary =
            ReadingSessionSummary(
                bookTitle = finalReader.document.summary.title,
                coverImagePath = finalReader.document.summary.coverImagePath,
                elapsedMillis = cycleStats.activeDurationMillis,
                wordsRead = cycleStats.wordsRead,
                averageWordsPerMinute = average,
                progress = finalReader.progress,
                pauses = cycleStats.pauses,
                backwardsMoves = cycleStats.backwardsMoves,
                fragmentsRead = cycleStats.fragmentsRead,
            )
        mutableState.update {
            it.copy(
                screen =
                    AppScreen.SessionResult(
                        bookId = finalReader.document.summary.id,
                        summary = summary,
                        previousReadings = finalReader.document.summary.completedReadings,
                        restartAvailable = finalReader.completed,
                    ),
            )
        }
    }

    fun restartCompletedBook(bookId: String) {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null) }
            runCatching { repository.restartBook(bookId) }
                .onSuccess { openBook(bookId) }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            busy = false,
                            message = error.userMessage("No fue posible reiniciar el libro"),
                        )
                    }
                }
        }
    }

    fun returnToLibrary() {
        viewModelScope.launch {
            reloadLibrary()
            mutableState.update { it.copy(screen = AppScreen.Library) }
        }
    }

    fun consumeMessage() {
        mutableState.update { it.copy(message = null) }
    }

    private fun moveToBlock(
        index: Int,
        backwards: Boolean,
        nowMillis: Long = SystemClock.elapsedRealtime(),
    ) {
        updateReader { reader ->
            val movedForward = index > reader.currentBlockIndex
            reader.copy(
                currentBlockIndex = index,
                furthestBlockIndex = maxOf(reader.furthestBlockIndex, index),
                backwardsMoves = reader.backwardsMoves + if (backwards) 1 else 0,
                completed = false,
                blockShownAtMillis = nowMillis,
                adaptationCooldownBlocks =
                    if (movedForward) {
                        (reader.adaptationCooldownBlocks - 1).coerceAtLeast(0)
                    } else {
                        reader.adaptationCooldownBlocks
                    },
            )
        }
        scheduleProgressSave()
    }

    private fun pauseFocusSession() {
        updateReader { reader ->
            val added = reader.playStartedAtMillis?.let { SystemClock.elapsedRealtime() - it } ?: 0L
            reader.copy(
                activeDurationMillis = reader.activeDurationMillis + added,
                playStartedAtMillis = null,
            )
        }
        persistProgressImmediately()
    }

    private fun scheduleProgressSave() {
        if (currentReader()?.quotePreview == true) return
        progressSaveJob?.cancel()
        progressSaveJob =
            viewModelScope.launch {
                delay(750)
                persistProgress()
            }
    }

    private fun persistProgressImmediately() {
        progressSaveJob?.cancel()
        val reader = currentReader() ?: return
        if (reader.quotePreview) return
        val bookId = reader.document.summary.id
        val characterOffset = reader.currentCharacterOffset
        progressSaveJob =
            viewModelScope.launch {
                repository.saveProgress(bookId, characterOffset)
            }
    }

    private suspend fun persistProgress() {
        val reader = currentReader() ?: return
        if (reader.quotePreview) return
        repository.saveProgress(reader.document.summary.id, reader.currentCharacterOffset)
    }

    private fun saveSettings(settings: ReaderSettings) {
        viewModelScope.launch { preferences.updateReaderSettings(settings) }
    }

    private fun persistReadingCycleStats() {
        val reader = currentReader()?.takeUnless { it.completed || it.quotePreview } ?: return
        val stats = reader.aggregateCycleStats()
        viewModelScope.launch {
            repository.saveReadingCycleStats(reader.document.summary.id, stats)
        }
    }

    private suspend fun reloadLibrary() {
        val books = repository.listBooks()
        val customAvatarPath =
            app.customAvatarRepository.avatarFile
                .takeIf { it.isFile }
                ?.absolutePath
        mutableState.update {
            it.copy(
                books = books,
                customNarratorAvatarPath = customAvatarPath,
                busy = false,
            )
        }
    }

    private fun currentReader(): ReaderState? = (mutableState.value.screen as? AppScreen.Reader)?.state

    private fun updateReader(transform: (ReaderState) -> ReaderState) {
        mutableState.update { ui ->
            val screen = ui.screen as? AppScreen.Reader ?: return@update ui
            ui.copy(screen = AppScreen.Reader(transform(screen.state)))
        }
    }

    private companion object {
        const val MINIMUM_AMBIENT_AUDIO_VOLUME_PERCENT = 0
        const val MAXIMUM_AMBIENT_AUDIO_VOLUME_PERCENT = 50
    }

    private fun completeReading(reader: ReaderState) {
        if (reader.completed || reader.quotePreview || reader.plan.blocks.isEmpty()) return
        val completedReader =
            reader.copy(
                currentBlockIndex = reader.plan.blocks.lastIndex,
                furthestBlockIndex = reader.plan.blocks.lastIndex,
                completed = true,
            )
        updateReader { completedReader }
        viewModelScope.launch {
            repository.completeReading(
                completedReader.document.summary.id,
                completedReader.toCompletedReading(System.currentTimeMillis()),
            )
        }
    }

    private fun Throwable.userMessage(fallback: String): String = message?.takeIf { it.isNotBlank() } ?: fallback

    private fun ReaderState.toCompletedReading(completedAtMillis: Long): CompletedReading {
        val aggregate = aggregateCycleStats()
        return CompletedReading(
            completedAtMillis = completedAtMillis,
            elapsedMillis = aggregate.activeDurationMillis,
            wordsRead = aggregate.wordsRead,
            averageWordsPerMinute = aggregate.averageWordsPerMinute(),
            pauses = aggregate.pauses,
            backwardsMoves = aggregate.backwardsMoves,
            fragmentsRead = aggregate.fragmentsRead,
        )
    }

    private fun ReaderState.aggregateCycleStats(): ReadingCycleStats {
        val first = sessionStartBlockIndex.coerceAtMost(furthestBlockIndex)
        val sessionWords =
            plan.blocks
                .subList(first, (furthestBlockIndex + 1).coerceAtMost(plan.blocks.size))
                .sumOf { it.wordCount }
        val sessionElapsed =
            activeDurationMillis +
                (playStartedAtMillis?.let { SystemClock.elapsedRealtime() - it } ?: 0L)
        val previous = document.summary.currentCycleStats
        return ReadingCycleStats(
            activeDurationMillis = previous.activeDurationMillis + sessionElapsed.coerceAtLeast(0),
            wordsRead = previous.wordsRead + sessionWords,
            pauses = previous.pauses + pauses,
            backwardsMoves = previous.backwardsMoves + backwardsMoves,
            fragmentsRead =
                previous.fragmentsRead +
                    (furthestBlockIndex - first + 1).coerceAtLeast(0),
        )
    }

    private fun ReadingCycleStats.averageWordsPerMinute(): Int =
        if (activeDurationMillis < 1_000 || wordsRead == 0) {
            0
        } else {
            (wordsRead * 60_000.0 / activeDurationMillis).roundToInt()
        }

    private fun completedBookResult(book: BookSummary): AppScreen.SessionResult {
        val latest = book.completedReadings.lastOrNull()
        val summary =
            ReadingSessionSummary(
                bookTitle = book.title,
                coverImagePath = book.coverImagePath,
                elapsedMillis = latest?.elapsedMillis ?: 0,
                wordsRead = latest?.wordsRead ?: book.totalWords,
                averageWordsPerMinute = latest?.averageWordsPerMinute ?: 0,
                progress = 1f,
                pauses = latest?.pauses ?: 0,
                backwardsMoves = latest?.backwardsMoves ?: 0,
                fragmentsRead = latest?.fragmentsRead ?: 0,
            )
        return AppScreen.SessionResult(
            bookId = book.id,
            summary = summary,
            previousReadings = book.completedReadings.dropLast(1).asReversed(),
            restartAvailable = true,
        )
    }

    override fun onCleared() {
        progressSaveJob?.cancel()
    }
}
