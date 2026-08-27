package com.octomind.booksreader.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.octomind.booksreader.OctomindApplication
import com.octomind.booksreader.domain.BookDocument
import com.octomind.booksreader.domain.BookSummary
import com.octomind.booksreader.domain.PageTheme
import com.octomind.booksreader.domain.NarratorAvatar
import com.octomind.booksreader.domain.ReaderFontStyle
import com.octomind.booksreader.domain.ReaderSettings
import com.octomind.booksreader.domain.ReadingPlan
import com.octomind.booksreader.domain.ReadingPlanBuilder
import com.octomind.booksreader.domain.ReadingMode
import com.octomind.booksreader.domain.ReadingSessionSummary
import kotlin.math.roundToInt
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

sealed interface AppScreen {
    data object Starting : AppScreen
    data object Onboarding : AppScreen
    data object Library : AppScreen
    data class Reader(val state: ReaderState) : AppScreen
    data class SessionResult(val summary: ReadingSessionSummary) : AppScreen
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
) {
    val currentCharacterOffset: Int
        get() = if (completed) document.text.length
        else plan.blocks.getOrNull(currentBlockIndex)?.startCharacterOffset ?: 0

    val progress: Float
        get() = if (document.text.isEmpty()) 0f else {
            (currentCharacterOffset.toFloat() / document.text.length).coerceIn(0f, 1f)
        }
}

data class OctomindUiState(
    val screen: AppScreen = AppScreen.Starting,
    val books: List<BookSummary> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
)

class OctomindViewModel(application: Application) : AndroidViewModel(application) {
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
                }
                .onFailure { error ->
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
                val settings = preferences.readerSettings.first().copy(
                    readingMode = ReadingMode.SENTENCE,
                    adaptivePacingEnabled = false,
                )
                val plan = withContext(Dispatchers.Default) {
                    ReadingPlanBuilder.build(
                        text = document.text,
                        wordsPerBlock = settings.wordsPerBlock,
                        readingMode = settings.readingMode,
                    )
                }
                require(plan.blocks.isNotEmpty()) { "El libro no contiene palabras legibles" }
                val currentBlockIndex = plan.blockIndexFor(document.summary.currentCharacterOffset)
                val restoredFocusEnabled = settings.focusEnabled
                val restoredSettings = settings.copy(
                    narratorAvatar = document.summary.narratorAvatar,
                    readerControlsExpanded = if (restoredFocusEnabled) {
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
                    customNarratorAvatarPath = app.customAvatarRepository.avatarFile
                        .takeIf { it.isFile }
                        ?.absolutePath,
                    currentBlockIndex = currentBlockIndex,
                    focusEnabled = restoredFocusEnabled,
                    completed = document.summary.currentCharacterOffset >= document.text.length,
                    blockShownAtMillis = now,
                    playStartedAtMillis = now.takeIf { restoredFocusEnabled },
                )
            }.onSuccess { reader ->
                mutableState.update { it.copy(screen = AppScreen.Reader(reader), busy = false) }
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

    fun toggleFocus() {
        val reader = currentReader() ?: return
        if (reader.focusEnabled) {
            pauseFocusSession()
            val settings = (currentReader() ?: reader).settings.copy(focusEnabled = false)
            updateReader { it.copy(focusEnabled = false, settings = settings) }
            saveSettings(settings)
        } else {
            val now = SystemClock.elapsedRealtime()
            val settings = reader.settings.copy(
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
    }

    fun resumeForForeground() {
        updateReader { reader -> if (reader.focusEnabled && reader.playStartedAtMillis == null) {
            reader.copy(playStartedAtMillis = SystemClock.elapsedRealtime())
        } else reader }
    }

    fun moveBlock(delta: Int) {
        val reader = currentReader() ?: return
        if (reader.plan.blocks.isEmpty()) return
        val next = (reader.currentBlockIndex + delta).coerceIn(0, reader.plan.blocks.lastIndex)
        if (next == reader.currentBlockIndex) return

        moveToBlock(next, backwards = delta < 0)
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
                val settings = latestReader.settings.copy(
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
                val settings = latestReader.settings.copy(
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

    fun finishReader() {
        val reader = currentReader() ?: return
        pauseFocusSession()
        val finalReader = currentReader() ?: reader
        persistProgressImmediately()
        val elapsed = finalReader.activeDurationMillis.coerceAtLeast(0)
        val first = finalReader.sessionStartBlockIndex.coerceAtMost(finalReader.furthestBlockIndex)
        val words = finalReader.plan.blocks
            .subList(first, (finalReader.furthestBlockIndex + 1).coerceAtMost(finalReader.plan.blocks.size))
            .sumOf { it.wordCount }
        val average = if (elapsed < 1_000 || words == 0) 0 else {
            (words * 60_000.0 / elapsed).roundToInt()
        }
        val summary = ReadingSessionSummary(
            bookTitle = finalReader.document.summary.title,
            coverImagePath = finalReader.document.summary.coverImagePath,
            elapsedMillis = elapsed,
            wordsRead = words,
            averageWordsPerMinute = average,
            progress = finalReader.progress,
            pauses = finalReader.pauses,
            backwardsMoves = finalReader.backwardsMoves,
            fragmentsRead = (finalReader.furthestBlockIndex - first + 1).coerceAtLeast(0),
        )
        mutableState.update { it.copy(screen = AppScreen.SessionResult(summary)) }
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
                adaptationCooldownBlocks = if (movedForward) {
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
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            delay(750)
            persistProgress()
        }
    }

    private fun persistProgressImmediately() {
        progressSaveJob?.cancel()
        val reader = currentReader() ?: return
        val bookId = reader.document.summary.id
        val characterOffset = reader.currentCharacterOffset
        progressSaveJob = viewModelScope.launch {
            repository.saveProgress(bookId, characterOffset)
        }
    }

    private suspend fun persistProgress() {
        val reader = currentReader() ?: return
        repository.saveProgress(reader.document.summary.id, reader.currentCharacterOffset)
    }

    private fun saveSettings(settings: ReaderSettings) {
        viewModelScope.launch { preferences.updateReaderSettings(settings) }
    }

    private suspend fun reloadLibrary() {
        val books = repository.listBooks()
        mutableState.update { it.copy(books = books, busy = false) }
    }

    private fun currentReader(): ReaderState? =
        (mutableState.value.screen as? AppScreen.Reader)?.state

    private fun updateReader(transform: (ReaderState) -> ReaderState) {
        mutableState.update { ui ->
            val screen = ui.screen as? AppScreen.Reader ?: return@update ui
            ui.copy(screen = AppScreen.Reader(transform(screen.state)))
        }
    }

    private fun Throwable.userMessage(fallback: String): String =
        message?.takeIf { it.isNotBlank() } ?: fallback

    override fun onCleared() {
        progressSaveJob?.cancel()
    }
}
