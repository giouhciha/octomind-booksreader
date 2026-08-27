package com.octomind.booksreader.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.octomind.booksreader.domain.PageTheme
import com.octomind.booksreader.domain.FocusPresentation
import com.octomind.booksreader.domain.AmbientIntensity
import com.octomind.booksreader.domain.NarratorAvatar
import com.octomind.booksreader.domain.ReaderFontStyle
import com.octomind.booksreader.domain.ReaderProfile
import com.octomind.booksreader.domain.ReaderSettings
import com.octomind.booksreader.domain.ReadingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore(name = "reader_preferences")

class UserPreferences(private val context: Context) {
    val adultConfirmed: Flow<Boolean> = context.userDataStore.data.map { preferences ->
        preferences[ADULT_CONFIRMED] ?: false
    }

    val readerSettings: Flow<ReaderSettings> = context.userDataStore.data.map { preferences ->
        ReaderSettings(
            wordsPerMinute = preferences[WORDS_PER_MINUTE] ?: 260,
            wordsPerBlock = preferences[WORDS_PER_BLOCK] ?: 4,
            readingMode = enumPreference(preferences[READING_MODE], ReadingMode.FIXED_WORDS),
            pageTheme = enumPreference(preferences[PAGE_THEME], PageTheme.LIGHT),
            fontStyle = enumPreference(preferences[FONT_STYLE], ReaderFontStyle.SERIF),
            fontSizeSp = (preferences[FONT_SIZE_SP] ?: 19).coerceIn(14, 32),
            adaptivePacingEnabled = preferences[ADAPTIVE_PACING_ENABLED] ?: true,
            focusDimmingPercent = (preferences[FOCUS_DIMMING_PERCENT] ?: 45).coerceIn(0, 80),
            showFocusMascot = preferences[SHOW_FOCUS_MASCOT] ?: true,
            focusPresentation = enumPreference(
                preferences[FOCUS_PRESENTATION],
                FocusPresentation.OCTI_NARRATOR,
            ),
            narratorAvatar = enumPreference(
                preferences[NARRATOR_AVATAR],
                NarratorAvatar.OCTI,
            ),
            customNarratorAvatarVersion = preferences[CUSTOM_NARRATOR_AVATAR_VERSION] ?: 0,
            ambientIntensity = enumPreference(
                preferences[AMBIENT_INTENSITY],
                AmbientIntensity.SUBTLE,
            ),
            focusEnabled = preferences[FOCUS_ENABLED] ?: false,
            readerControlsExpanded = preferences[READER_CONTROLS_EXPANDED] ?: true,
            narratorGestureHintDismissed = preferences[NARRATOR_GESTURE_HINT_DISMISSED] ?: false,
        )
    }

    val readerProfile: Flow<ReaderProfile> = context.userDataStore.data.map { preferences ->
        ReaderProfile(
            baselineWordsPerMinute = preferences[PROFILE_BASELINE_WPM] ?: 260,
            calibrationSampleCount = preferences[PROFILE_SAMPLE_COUNT] ?: 0,
            completedCalibrations = preferences[PROFILE_CALIBRATIONS] ?: 0,
        )
    }

    suspend fun confirmAdult() {
        context.userDataStore.edit { it[ADULT_CONFIRMED] = true }
    }

    suspend fun updateReaderSettings(settings: ReaderSettings) {
        context.userDataStore.edit { preferences ->
            preferences[WORDS_PER_MINUTE] = settings.wordsPerMinute.coerceIn(80, 1200)
            preferences[WORDS_PER_BLOCK] = settings.wordsPerBlock.coerceIn(1, 8)
            preferences[READING_MODE] = settings.readingMode.name
            preferences[PAGE_THEME] = settings.pageTheme.name
            preferences[FONT_STYLE] = settings.fontStyle.name
            preferences[FONT_SIZE_SP] = settings.fontSizeSp.coerceIn(14, 32)
            preferences[ADAPTIVE_PACING_ENABLED] = settings.adaptivePacingEnabled
            preferences[FOCUS_DIMMING_PERCENT] = settings.focusDimmingPercent.coerceIn(0, 80)
            preferences[SHOW_FOCUS_MASCOT] = settings.showFocusMascot
            preferences[FOCUS_PRESENTATION] = settings.focusPresentation.name
            preferences[NARRATOR_AVATAR] = settings.narratorAvatar.name
            preferences[CUSTOM_NARRATOR_AVATAR_VERSION] =
                settings.customNarratorAvatarVersion.coerceAtLeast(0)
            preferences[AMBIENT_INTENSITY] = settings.ambientIntensity.name
            preferences[FOCUS_ENABLED] = settings.focusEnabled
            preferences[READER_CONTROLS_EXPANDED] = settings.readerControlsExpanded
            preferences[NARRATOR_GESTURE_HINT_DISMISSED] = settings.narratorGestureHintDismissed
        }
    }

    suspend fun recordCalibration(estimatedWordsPerMinute: Int, sampleCount: Int): ReaderProfile {
        var updatedProfile = ReaderProfile()
        context.userDataStore.edit { preferences ->
            val previousSamples = preferences[PROFILE_SAMPLE_COUNT] ?: 0
            val previousBaseline = preferences[PROFILE_BASELINE_WPM] ?: 260
            val acceptedSamples = sampleCount.coerceAtLeast(0)
            val totalSamples = previousSamples + acceptedSamples
            val blendedBaseline = if (totalSamples == 0) {
                estimatedWordsPerMinute
            } else {
                ((previousBaseline.toLong() * previousSamples) +
                    (estimatedWordsPerMinute.toLong() * acceptedSamples)) / totalSamples
            }.toInt().coerceIn(100, 700)
            val calibrations = (preferences[PROFILE_CALIBRATIONS] ?: 0) + 1

            preferences[PROFILE_BASELINE_WPM] = blendedBaseline
            preferences[PROFILE_SAMPLE_COUNT] = totalSamples
            preferences[PROFILE_CALIBRATIONS] = calibrations
            preferences[WORDS_PER_MINUTE] = blendedBaseline
            updatedProfile = ReaderProfile(
                baselineWordsPerMinute = blendedBaseline,
                calibrationSampleCount = totalSamples,
                completedCalibrations = calibrations,
            )
        }
        return updatedProfile
    }

    private companion object {
        inline fun <reified T : Enum<T>> enumPreference(value: String?, fallback: T): T =
            runCatching { enumValueOf<T>(value.orEmpty()) }.getOrDefault(fallback)

        val ADULT_CONFIRMED = booleanPreferencesKey("adult_confirmed")
        val WORDS_PER_MINUTE = intPreferencesKey("words_per_minute")
        val WORDS_PER_BLOCK = intPreferencesKey("words_per_block")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val PAGE_THEME = stringPreferencesKey("page_theme")
        val FONT_STYLE = stringPreferencesKey("font_style")
        val FONT_SIZE_SP = intPreferencesKey("font_size_sp")
        val ADAPTIVE_PACING_ENABLED = booleanPreferencesKey("adaptive_pacing_enabled")
        val FOCUS_DIMMING_PERCENT = intPreferencesKey("focus_dimming_percent")
        val SHOW_FOCUS_MASCOT = booleanPreferencesKey("show_focus_mascot")
        // La clave v2 establece Octi narrador como experiencia inicial para instalaciones
        // que conocieron la primera mascota antes de que existiera el selector de estilo.
        val FOCUS_PRESENTATION = stringPreferencesKey("focus_presentation_v2")
        val NARRATOR_AVATAR = stringPreferencesKey("narrator_avatar")
        val CUSTOM_NARRATOR_AVATAR_VERSION = intPreferencesKey("custom_narrator_avatar_version")
        val AMBIENT_INTENSITY = stringPreferencesKey("ambient_intensity")
        val FOCUS_ENABLED = booleanPreferencesKey("focus_enabled")
        val READER_CONTROLS_EXPANDED = booleanPreferencesKey("reader_controls_expanded")
        val NARRATOR_GESTURE_HINT_DISMISSED = booleanPreferencesKey("narrator_gesture_hint_dismissed")
        val PROFILE_BASELINE_WPM = intPreferencesKey("profile_baseline_wpm")
        val PROFILE_SAMPLE_COUNT = intPreferencesKey("profile_sample_count")
        val PROFILE_CALIBRATIONS = intPreferencesKey("profile_calibrations")
    }
}
