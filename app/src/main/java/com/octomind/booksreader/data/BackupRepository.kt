package com.octomind.booksreader.data

import android.content.Context
import android.net.Uri
import com.octomind.booksreader.domain.AmbientIntensity
import com.octomind.booksreader.domain.FocusPresentation
import com.octomind.booksreader.domain.NarratorAvatar
import com.octomind.booksreader.domain.PageTheme
import com.octomind.booksreader.domain.ReaderFontStyle
import com.octomind.booksreader.domain.ReaderProfile
import com.octomind.booksreader.domain.ReaderSettings
import com.octomind.booksreader.domain.ReadingMode
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class BackupRepository(
    private val context: Context,
    private val preferences: UserPreferences,
) {
    suspend fun create(uri: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        val snapshot = preferences.snapshot()
        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
            EncryptedBackupCodec.encrypt(password, output) { encryptedOutput ->
                val zip = ZipOutputStream(encryptedOutput)
                writeText(zip, MANIFEST_ENTRY, manifestJson().toString())
                writeText(zip, PREFERENCES_ENTRY, snapshot.toJson().toString())
                addDirectory(zip, File(context.filesDir, LIBRARY_DIRECTORY), "$LIBRARY_DIRECTORY/")
                addDirectory(zip, File(context.filesDir, AVATAR_DIRECTORY), "$AVATAR_DIRECTORY/")
                zip.finish()
                zip.flush()
            }
        } ?: error("No fue posible crear el archivo de respaldo")
    }

    suspend fun restore(uri: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "backup-restore-${System.nanoTime()}")
        val extracted = File(staging, "content")
        try {
            extracted.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                EncryptedBackupCodec.decrypt(password, input) { decrypted -> extract(decrypted, extracted) }
            } ?: error("No fue posible abrir el respaldo")
            val manifest = File(extracted, MANIFEST_ENTRY)
            require(manifest.isFile) { "El respaldo no contiene un manifiesto válido" }
            require(JSONObject(manifest.readText()).optInt("schemaVersion") == SCHEMA_VERSION) {
                "Esta versión del respaldo no es compatible"
            }
            val restoredPreferences = File(extracted, PREFERENCES_ENTRY)
            require(restoredPreferences.isFile) { "El respaldo no contiene las preferencias" }
            val snapshot = JSONObject(restoredPreferences.readText()).toPreferencesSnapshot()
            validateLibrary(File(extracted, LIBRARY_DIRECTORY))
            replacePrivateDirectory(LIBRARY_DIRECTORY, File(extracted, LIBRARY_DIRECTORY))
            replacePrivateDirectory(AVATAR_DIRECTORY, File(extracted, AVATAR_DIRECTORY))
            preferences.restore(snapshot)
        } finally {
            staging.deleteRecursively()
            password.fill('\u0000')
        }
    }

    private fun extract(input: InputStream, destination: File) {
        var entryCount = 0
        var totalBytes = 0L
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryCount += 1
                require(entryCount <= MAX_ENTRIES) { "El respaldo contiene demasiados archivos" }
                val target = safeTarget(destination, entry.name)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var read = zip.read(buffer)
                        while (read >= 0) {
                            totalBytes += read
                            require(totalBytes <= MAX_RESTORED_BYTES) { "El respaldo supera el tamaño permitido" }
                            output.write(buffer, 0, read)
                            read = zip.read(buffer)
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun validateLibrary(directory: File) {
        if (!directory.exists()) return
        require(directory.isDirectory) { "La biblioteca del respaldo no es válida" }
        val metadata = File(directory, "books.json")
        if (metadata.exists()) require(runCatching { org.json.JSONArray(metadata.readText()) }.isSuccess) {
            "Los metadatos de la biblioteca están dañados"
        }
    }

    private fun replacePrivateDirectory(name: String, restored: File) {
        val current = File(context.filesDir, name)
        val previous = File(context.filesDir, "$name.backup-previous")
        previous.deleteRecursively()
        if (current.exists()) check(current.renameTo(previous)) { "No fue posible preparar la restauración" }
        try {
            if (restored.exists()) check(restored.renameTo(current)) { "No fue posible restaurar $name" }
            previous.deleteRecursively()
        } catch (error: Throwable) {
            current.deleteRecursively()
            if (previous.exists()) previous.renameTo(current)
            throw error
        }
    }

    private fun addDirectory(zip: ZipOutputStream, directory: File, prefix: String) {
        if (!directory.isDirectory) return
        directory.walkTopDown().filter(File::isFile).forEach { file ->
            val relative = file.relativeTo(directory).invariantSeparatorsPath
            zip.putNextEntry(ZipEntry(prefix + relative))
            file.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }

    private fun writeText(zip: ZipOutputStream, name: String, value: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(value.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun safeTarget(root: File, name: String): File {
        require(name.isNotBlank() && !name.startsWith('/')) { "Ruta no válida en el respaldo" }
        val target = File(root, name).canonicalFile
        val rootPath = root.canonicalFile.path + File.separator
        require(target.path.startsWith(rootPath)) { "Ruta no segura en el respaldo" }
        return target
    }

    private fun manifestJson() = JSONObject()
        .put("schemaVersion", SCHEMA_VERSION)
        .put("createdAtMillis", System.currentTimeMillis())
        .put("application", "Octomind Books Reader")

    private companion object {
        const val SCHEMA_VERSION = 1
        const val MANIFEST_ENTRY = "manifest.json"
        const val PREFERENCES_ENTRY = "preferences.json"
        const val LIBRARY_DIRECTORY = "library"
        const val AVATAR_DIRECTORY = "narrator_avatars"
        const val MAX_ENTRIES = 10_000
        const val MAX_RESTORED_BYTES = 2L * 1024L * 1024L * 1024L
    }
}

private fun UserPreferencesSnapshot.toJson() = JSONObject()
    .put("adultConfirmed", adultConfirmed)
    .put("readerSettings", readerSettings.toJson())
    .put("readerProfile", readerProfile.toJson())

private fun ReaderSettings.toJson() = JSONObject()
    .put("wordsPerMinute", wordsPerMinute)
    .put("wordsPerBlock", wordsPerBlock)
    .put("readingMode", readingMode.name)
    .put("pageTheme", pageTheme.name)
    .put("fontStyle", fontStyle.name)
    .put("fontSizeSp", fontSizeSp)
    .put("adaptivePacingEnabled", adaptivePacingEnabled)
    .put("focusDimmingPercent", focusDimmingPercent)
    .put("showFocusMascot", showFocusMascot)
    .put("focusPresentation", focusPresentation.name)
    .put("narratorAvatar", narratorAvatar.name)
    .put("customNarratorAvatarVersion", customNarratorAvatarVersion)
    .put("ambientIntensity", ambientIntensity.name)
    .put("focusEnabled", focusEnabled)
    .put("readerControlsExpanded", readerControlsExpanded)
    .put("narratorGestureHintDismissed", narratorGestureHintDismissed)

private fun ReaderProfile.toJson() = JSONObject()
    .put("baselineWordsPerMinute", baselineWordsPerMinute)
    .put("calibrationSampleCount", calibrationSampleCount)
    .put("completedCalibrations", completedCalibrations)

private fun JSONObject.toPreferencesSnapshot(): UserPreferencesSnapshot {
    val settings = getJSONObject("readerSettings")
    val profile = getJSONObject("readerProfile")
    return UserPreferencesSnapshot(
        adultConfirmed = optBoolean("adultConfirmed", true),
        readerSettings = ReaderSettings(
            wordsPerMinute = settings.optInt("wordsPerMinute", 260),
            wordsPerBlock = settings.optInt("wordsPerBlock", 4),
            readingMode = settings.enum("readingMode", ReadingMode.FIXED_WORDS),
            pageTheme = settings.enum("pageTheme", PageTheme.LIGHT),
            fontStyle = settings.enum("fontStyle", ReaderFontStyle.SERIF),
            fontSizeSp = settings.optInt("fontSizeSp", 19),
            adaptivePacingEnabled = settings.optBoolean("adaptivePacingEnabled", true),
            focusDimmingPercent = settings.optInt("focusDimmingPercent", 45),
            showFocusMascot = settings.optBoolean("showFocusMascot", true),
            focusPresentation = settings.enum("focusPresentation", FocusPresentation.OCTI_NARRATOR),
            narratorAvatar = settings.enum("narratorAvatar", NarratorAvatar.OCTI),
            customNarratorAvatarVersion = settings.optInt("customNarratorAvatarVersion", 0),
            ambientIntensity = settings.enum("ambientIntensity", AmbientIntensity.SUBTLE),
            focusEnabled = settings.optBoolean("focusEnabled", false),
            readerControlsExpanded = settings.optBoolean("readerControlsExpanded", true),
            narratorGestureHintDismissed = settings.optBoolean("narratorGestureHintDismissed", false),
        ),
        readerProfile = ReaderProfile(
            baselineWordsPerMinute = profile.optInt("baselineWordsPerMinute", 260),
            calibrationSampleCount = profile.optInt("calibrationSampleCount", 0),
            completedCalibrations = profile.optInt("completedCalibrations", 0),
        ),
    )
}

private inline fun <reified T : Enum<T>> JSONObject.enum(name: String, fallback: T): T =
    runCatching { enumValueOf<T>(optString(name)) }.getOrDefault(fallback)
