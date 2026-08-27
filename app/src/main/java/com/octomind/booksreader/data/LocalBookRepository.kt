package com.octomind.booksreader.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.octomind.booksreader.domain.BookChapter
import com.octomind.booksreader.domain.BookDocument
import com.octomind.booksreader.domain.BookFormat
import com.octomind.booksreader.domain.BookSummary
import com.octomind.booksreader.domain.NarratorAvatar
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LocalBookRepository(private val context: Context) {
    private val lock = Any()
    private val libraryDirectory = File(context.filesDir, "library")
    private val metadataFile = File(libraryDirectory, "books.json")

    suspend fun listBooks(): List<BookSummary> = withContext(Dispatchers.IO) {
        synchronized(lock) {
            readRecords().map { it.toSummary(libraryDirectory) }
                .sortedByDescending(BookSummary::lastOpenedAtMillis)
        }
    }

    suspend fun importBook(uri: Uri): BookSummary = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri)
        val extension = displayName.substringAfterLast('.', "").lowercase()
        require(extension == "txt" || extension == "epub") { "Por ahora puedes importar archivos TXT o EPUB" }

        libraryDirectory.mkdirs()
        val temporaryFile = File.createTempFile("import-", ".$extension", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temporaryFile.outputStream().use { output -> input.copyTo(output) }
            } ?: error("No fue posible abrir el archivo")

            val parser: BookParser = if (extension == "epub") EpubBookParser() else TxtBookParser()
            val parsed = parser.parse(temporaryFile, displayName)
            val id = UUID.randomUUID().toString()
            val contentFile = File(libraryDirectory, "$id.txt")
            contentFile.writeText(parsed.text, Charsets.UTF_8)
            val coverFile = parsed.coverImage?.takeIf(::isSafeCoverImage)?.let { bytes ->
                File(libraryDirectory, "$id.cover").also { it.writeBytes(bytes) }
            }
            val now = System.currentTimeMillis()
            val record = BookRecord(
                id = id,
                title = parsed.title,
                author = parsed.author,
                format = parsed.format,
                totalWords = countWords(parsed.text),
                totalCharacters = parsed.text.length,
                currentCharacterOffset = 0,
                lastOpenedAtMillis = now,
                calibrationCompleted = true,
                contentFileName = contentFile.name,
                chapters = parsed.chapters,
                coverFileName = coverFile?.name,
                narratorAvatar = NarratorAvatar.OCTI,
            )
            synchronized(lock) {
                val records = readRecords().toMutableList().apply { add(record) }
                writeRecords(records)
            }
            record.toSummary(libraryDirectory)
        } finally {
            temporaryFile.delete()
        }
    }

    suspend fun loadBook(id: String): BookDocument = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val records = readRecords().toMutableList()
            val index = records.indexOfFirst { it.id == id }
            require(index >= 0) { "El libro ya no está disponible" }
            val opened = records[index].copy(lastOpenedAtMillis = System.currentTimeMillis())
            records[index] = opened
            writeRecords(records)
            val contentFile = File(libraryDirectory, opened.contentFileName)
            require(contentFile.exists()) { "No se encontró el contenido local del libro" }
            BookDocument(
                summary = opened.toSummary(libraryDirectory),
                text = contentFile.readText(Charsets.UTF_8),
                chapters = opened.chapters,
            )
        }
    }

    suspend fun saveProgress(id: String, characterOffset: Int) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val records = readRecords().toMutableList()
            val index = records.indexOfFirst { it.id == id }
            if (index >= 0) {
                records[index] = records[index].copy(
                    currentCharacterOffset = characterOffset.coerceAtLeast(0),
                    lastOpenedAtMillis = System.currentTimeMillis(),
                )
                writeRecords(records)
            }
        }
    }

    suspend fun saveNarratorAvatar(id: String, avatar: NarratorAvatar) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val records = readRecords().toMutableList()
            val index = records.indexOfFirst { it.id == id }
            if (index >= 0) {
                records[index] = records[index].copy(narratorAvatar = avatar)
                writeRecords(records)
            }
        }
    }

    suspend fun replaceNarratorAvatarForAll(from: NarratorAvatar, to: NarratorAvatar) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val records = readRecords()
                if (records.any { it.narratorAvatar == from }) {
                    writeRecords(records.map { record ->
                        if (record.narratorAvatar == from) record.copy(narratorAvatar = to) else record
                    })
                }
            }
        }

    suspend fun deleteBook(id: String) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val records = readRecords().toMutableList()
            val removed = records.firstOrNull { it.id == id } ?: return@synchronized
            File(libraryDirectory, removed.contentFileName).delete()
            removed.coverFileName?.let { File(libraryDirectory, it).delete() }
            records.removeAll { it.id == id }
            writeRecords(records)
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "libro.txt"
    }

    private fun readRecords(): List<BookRecord> {
        if (!metadataFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(metadataFile.readText(Charsets.UTF_8))
            (0 until array.length()).map { index -> BookRecord.fromJson(array.getJSONObject(index)) }
        }.getOrElse { emptyList() }
    }

    private fun writeRecords(records: List<BookRecord>) {
        libraryDirectory.mkdirs()
        val array = JSONArray().apply { records.forEach { put(it.toJson()) } }
        val temporary = File(libraryDirectory, "books.json.tmp")
        temporary.writeText(array.toString(), Charsets.UTF_8)
        if (!temporary.renameTo(metadataFile)) {
            temporary.copyTo(metadataFile, overwrite = true)
            temporary.delete()
        }
    }

    private fun countWords(text: String): Int = Regex("\\S+").findAll(text).count()

    private fun isSafeCoverImage(bytes: ByteArray): Boolean {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outWidth in 64..8_192 &&
            options.outHeight in 64..8_192 &&
            options.outWidth.toLong() * options.outHeight.toLong() <= 40_000_000L
    }
}

private data class BookRecord(
    val id: String,
    val title: String,
    val author: String?,
    val format: BookFormat,
    val totalWords: Int,
    val totalCharacters: Int,
    val currentCharacterOffset: Int,
    val lastOpenedAtMillis: Long,
    val calibrationCompleted: Boolean,
    val contentFileName: String,
    val chapters: List<BookChapter>,
    val coverFileName: String?,
    val narratorAvatar: NarratorAvatar,
) {
    fun toSummary(libraryDirectory: File) = BookSummary(
        id = id,
        title = title,
        author = author,
        format = format,
        totalWords = totalWords,
        totalCharacters = totalCharacters,
        currentCharacterOffset = currentCharacterOffset,
        lastOpenedAtMillis = lastOpenedAtMillis,
        calibrationCompleted = calibrationCompleted,
        coverImagePath = coverFileName?.let { File(libraryDirectory, it).absolutePath },
        narratorAvatar = narratorAvatar,
    )

    fun toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("author", author)
        put("format", format.name)
        put("totalWords", totalWords)
        put("totalCharacters", totalCharacters)
        put("currentCharacterOffset", currentCharacterOffset)
        put("lastOpenedAtMillis", lastOpenedAtMillis)
        put("calibrationCompleted", calibrationCompleted)
        put("contentFileName", contentFileName)
        put("coverFileName", coverFileName)
        put("narratorAvatar", narratorAvatar.name)
        put("chapters", JSONArray().apply {
            chapters.forEach { chapter ->
                put(JSONObject().apply {
                    put("title", chapter.title)
                    put("startCharacterOffset", chapter.startCharacterOffset)
                })
            }
        })
    }

    companion object {
        fun fromJson(json: JSONObject): BookRecord {
            val chapterArray = json.optJSONArray("chapters") ?: JSONArray()
            return BookRecord(
                id = json.getString("id"),
                title = json.getString("title"),
                author = json.optString("author").takeIf { it.isNotBlank() && it != "null" },
                format = BookFormat.valueOf(json.getString("format")),
                totalWords = json.getInt("totalWords"),
                totalCharacters = json.optInt("totalCharacters", json.getInt("totalWords") * 6),
                currentCharacterOffset = json.optInt("currentCharacterOffset", 0),
                lastOpenedAtMillis = json.optLong("lastOpenedAtMillis", 0),
                // Los libros creados antes de esta función no deben forzar una calibración retroactiva.
                calibrationCompleted = json.optBoolean("calibrationCompleted", true),
                contentFileName = json.getString("contentFileName"),
                coverFileName = json.optString("coverFileName")
                    .takeIf { it.isNotBlank() && it != "null" },
                narratorAvatar = runCatching {
                    NarratorAvatar.valueOf(json.optString("narratorAvatar"))
                }.getOrDefault(NarratorAvatar.OCTI),
                chapters = (0 until chapterArray.length()).map { index ->
                    chapterArray.getJSONObject(index).let { chapter ->
                        BookChapter(
                            title = chapter.getString("title"),
                            startCharacterOffset = chapter.getInt("startCharacterOffset"),
                        )
                    }
                },
            )
        }
    }
}
