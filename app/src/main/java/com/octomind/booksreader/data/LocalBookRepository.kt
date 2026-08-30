package com.octomind.booksreader.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.octomind.booksreader.domain.BookChapter
import com.octomind.booksreader.domain.BookDocument
import com.octomind.booksreader.domain.BookFormat
import com.octomind.booksreader.domain.BookPageAnchor
import com.octomind.booksreader.domain.BookSummary
import com.octomind.booksreader.domain.CompletedReading
import com.octomind.booksreader.domain.NarratorAvatar
import com.octomind.booksreader.domain.ReadingCycleStats
import com.octomind.booksreader.domain.SavedQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class LocalBookRepository(
    private val context: Context,
) {
    private val lock = Any()
    private val libraryDirectory = File(context.filesDir, "library")
    private val metadataFile = File(libraryDirectory, "books.json")

    suspend fun listBooks(): List<BookSummary> =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                readRecords()
                    .map { it.toSummary(libraryDirectory) }
                    .sortedByDescending(BookSummary::lastOpenedAtMillis)
            }
        }

    suspend fun importBook(uri: Uri): BookSummary =
        withContext(Dispatchers.IO) {
            val displayName = queryDisplayName(uri)
            val mimeType = context.contentResolver.getType(uri)
            queryFileSize(uri)?.takeIf { it >= 0 }?.let { size ->
                require(size in 1..MAX_IMPORT_BYTES) { "El archivo supera el límite de 200 MB" }
            }

            libraryDirectory.mkdirs()
            val temporaryFile = File.createTempFile("import-", ".book", context.cacheDir)
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temporaryFile.outputStream().use { output ->
                        input.copyToWithLimit(output)
                    }
                } ?: error("No fue posible abrir el archivo")

                val detectedFormat =
                    BookImportFormatDetector.detect(
                        displayName = displayName,
                        mimeType = mimeType,
                        header = temporaryFile.readHeader(),
                    )

                val parser: BookParser =
                    when (detectedFormat) {
                        BookFormat.EPUB -> EpubBookParser()
                        BookFormat.PDF -> PdfBookParser()
                        BookFormat.TXT -> TxtBookParser()
                    }
                val parsed = parser.parse(temporaryFile, displayName)
                val id = UUID.randomUUID().toString()
                val contentFile = File(libraryDirectory, "$id.txt")
                contentFile.writeText(parsed.text, Charsets.UTF_8)
                val originalFile =
                    if (parsed.format == BookFormat.PDF) {
                        File(libraryDirectory, "$id.pdf").also { temporaryFile.copyTo(it) }
                    } else {
                        null
                    }
                val coverFile =
                    parsed.coverImage?.takeIf(::isSafeCoverImage)?.let { bytes ->
                        File(libraryDirectory, "$id.cover").also { it.writeBytes(bytes) }
                    }
                val now = System.currentTimeMillis()
                val record =
                    BookRecord(
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
                        normalizationVersion = CURRENT_DOCUMENT_NORMALIZATION_VERSION,
                        chapters = parsed.chapters,
                        coverFileName = coverFile?.name,
                        originalFileName = originalFile?.name,
                        pageAnchors = parsed.pageAnchors,
                        narratorAvatar = NarratorAvatar.OCTI,
                        completedReadings = emptyList(),
                        currentCycleStats = ReadingCycleStats(),
                        savedQuotes = emptyList(),
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

    suspend fun loadBook(id: String): BookDocument =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val records = readRecords().toMutableList()
                val index = records.indexOfFirst { it.id == id }
                require(index >= 0) { "El libro ya no está disponible" }
                val refreshed = refreshPdfNormalization(records[index])
                val opened = refreshed.copy(lastOpenedAtMillis = System.currentTimeMillis())
                records[index] = opened
                writeRecords(records)
                val contentFile = File(libraryDirectory, opened.contentFileName)
                require(contentFile.exists()) { "No se encontró el contenido local del libro" }
                BookDocument(
                    summary = opened.toSummary(libraryDirectory),
                    text = contentFile.readText(Charsets.UTF_8),
                    chapters = opened.chapters,
                    originalFilePath =
                        opened.originalFileName
                            ?.let { File(libraryDirectory, it) }
                            ?.takeIf(File::isFile)
                            ?.absolutePath,
                    pageAnchors = opened.pageAnchors,
                )
            }
        }

    suspend fun saveProgress(
        id: String,
        characterOffset: Int,
    ) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val records = readRecords().toMutableList()
            val index = records.indexOfFirst { it.id == id }
            if (index >= 0) {
                records[index] =
                    records[index].copy(
                        currentCharacterOffset = characterOffset.coerceAtLeast(0),
                        lastOpenedAtMillis = System.currentTimeMillis(),
                    )
                writeRecords(records)
            }
        }
    }

    suspend fun completeReading(
        id: String,
        reading: CompletedReading,
    ) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val records = readRecords().toMutableList()
            val index = records.indexOfFirst { it.id == id }
            if (index >= 0) {
                val record = records[index]
                if (record.completedReadings.any { it.completedAtMillis == reading.completedAtMillis }) {
                    return@synchronized
                }
                records[index] =
                    record.copy(
                        currentCharacterOffset = record.totalCharacters,
                        lastOpenedAtMillis = System.currentTimeMillis(),
                        completedReadings = record.completedReadings + reading,
                        currentCycleStats = ReadingCycleStats(),
                    )
                writeRecords(records)
            }
        }
    }

    suspend fun saveReadingCycleStats(
        id: String,
        stats: ReadingCycleStats,
    ) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val records = readRecords().toMutableList()
            val index = records.indexOfFirst { it.id == id }
            if (index >= 0 && records[index].currentCharacterOffset < records[index].totalCharacters) {
                records[index] = records[index].copy(currentCycleStats = stats)
                writeRecords(records)
            }
        }
    }

    suspend fun restartBook(id: String) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val records = readRecords().toMutableList()
                val index = records.indexOfFirst { it.id == id }
                if (index >= 0) {
                    records[index] =
                        records[index].copy(
                            currentCharacterOffset = 0,
                            lastOpenedAtMillis = System.currentTimeMillis(),
                            currentCycleStats = ReadingCycleStats(),
                        )
                    writeRecords(records)
                }
            }
        }

    suspend fun saveNarratorAvatar(
        id: String,
        avatar: NarratorAvatar,
    ) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val records = readRecords().toMutableList()
            val index = records.indexOfFirst { it.id == id }
            if (index >= 0) {
                records[index] = records[index].copy(narratorAvatar = avatar)
                writeRecords(records)
            }
        }
    }

    suspend fun listQuotes(): List<SavedQuote> =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                readRecords().flatMap(BookRecord::savedQuotes).sortedByDescending(SavedQuote::createdAtMillis)
            }
        }

    suspend fun saveQuote(
        bookId: String,
        chapterTitle: String?,
        text: String,
        startCharacterOffset: Int,
        endCharacterOffset: Int,
    ): Boolean =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val records = readRecords().toMutableList()
                val index = records.indexOfFirst { it.id == bookId }
                if (index < 0) return@synchronized false
                val record = records[index]
                if (record.savedQuotes.any {
                        it.startCharacterOffset == startCharacterOffset &&
                            it.endCharacterOffset == endCharacterOffset
                    }
                ) {
                    return@synchronized false
                }
                val quote =
                    SavedQuote(
                        id = UUID.randomUUID().toString(),
                        bookId = record.id,
                        bookTitle = record.title,
                        chapterTitle = chapterTitle,
                        text = text.trim(),
                        startCharacterOffset = startCharacterOffset,
                        endCharacterOffset = endCharacterOffset,
                        createdAtMillis = System.currentTimeMillis(),
                    )
                records[index] = record.copy(savedQuotes = record.savedQuotes + quote)
                writeRecords(records)
                true
            }
        }

    suspend fun deleteQuote(
        bookId: String,
        quoteId: String,
    ) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val records = readRecords().toMutableList()
            val index = records.indexOfFirst { it.id == bookId }
            if (index >= 0) {
                records[index] =
                    records[index].copy(
                        savedQuotes = records[index].savedQuotes.filterNot { it.id == quoteId },
                    )
                writeRecords(records)
            }
        }
    }

    suspend fun replaceNarratorAvatarForAll(
        from: NarratorAvatar,
        to: NarratorAvatar,
    ) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val records = readRecords()
            if (records.any { it.narratorAvatar == from }) {
                writeRecords(
                    records.map { record ->
                        if (record.narratorAvatar == from) record.copy(narratorAvatar = to) else record
                    },
                )
            }
        }
    }

    suspend fun deleteBook(id: String) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val records = readRecords().toMutableList()
                val removed = records.firstOrNull { it.id == id } ?: return@synchronized
                File(libraryDirectory, removed.contentFileName).delete()
                removed.coverFileName?.let { File(libraryDirectory, it).delete() }
                removed.originalFileName?.let { File(libraryDirectory, it).delete() }
                records.removeAll { it.id == id }
                writeRecords(records)
            }
        }

    private fun queryDisplayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor
                    .getString(0)
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let { return it }
            }
        }
        return uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf(String::isNotBlank)
            ?: "libro"
    }

    private fun queryFileSize(uri: Uri): Long? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getLong(0)
        }
        return null
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

    private fun refreshPdfNormalization(record: BookRecord): BookRecord {
        if (record.format != BookFormat.PDF ||
            record.normalizationVersion >= CURRENT_DOCUMENT_NORMALIZATION_VERSION
        ) {
            return record
        }
        val originalFileName = record.originalFileName ?: return record
        val originalFile = File(libraryDirectory, originalFileName).takeIf(File::isFile) ?: return record
        return runCatching {
            val parsed = PdfBookParser().parse(originalFile, "${record.title}.pdf")
            val contentFile = File(libraryDirectory, record.contentFileName)
            val wasCompleted = record.currentCharacterOffset >= record.totalCharacters
            val currentPage =
                record.pageAnchors
                    .lastOrNull { it.startCharacterOffset <= record.currentCharacterOffset }
                    ?.pageIndex
            val refreshedOffset =
                when {
                    wasCompleted -> parsed.text.length
                    currentPage != null ->
                        parsed.pageAnchors
                            .lastOrNull { it.pageIndex <= currentPage }
                            ?.startCharacterOffset
                            ?: 0
                    record.totalCharacters > 0 ->
                        (
                            parsed.text.length * record.currentCharacterOffset.toDouble() / record.totalCharacters
                        ).toInt()
                    else -> 0
                }.coerceIn(0, parsed.text.length)
            contentFile.writeText(parsed.text, Charsets.UTF_8)
            record.copy(
                totalWords = countWords(parsed.text),
                totalCharacters = parsed.text.length,
                currentCharacterOffset = refreshedOffset,
                normalizationVersion = CURRENT_DOCUMENT_NORMALIZATION_VERSION,
                chapters = parsed.chapters,
                pageAnchors = parsed.pageAnchors,
                savedQuotes = record.savedQuotes.map { quote -> quote.remapTo(parsed.text) },
            )
        }.getOrElse { record }
    }

    private fun SavedQuote.remapTo(refreshedText: String): SavedQuote {
        val exactStart = refreshedText.indexOf(text)
        if (exactStart >= 0) {
            return copy(
                startCharacterOffset = exactStart,
                endCharacterOffset = exactStart + text.length,
            )
        }
        val safeStart = startCharacterOffset.coerceIn(0, refreshedText.length)
        val safeEnd = endCharacterOffset.coerceIn(safeStart, refreshedText.length)
        return copy(startCharacterOffset = safeStart, endCharacterOffset = safeEnd)
    }

    private fun isSafeCoverImage(bytes: ByteArray): Boolean {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outWidth in 64..8_192 &&
            options.outHeight in 64..8_192 &&
            options.outWidth.toLong() * options.outHeight.toLong() <= 40_000_000L
    }

    private fun InputStream.copyToWithLimit(output: OutputStream) {
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        var copiedBytes = 0L
        while (true) {
            val bytesRead = read(buffer)
            if (bytesRead < 0) return
            copiedBytes += bytesRead
            require(copiedBytes <= MAX_IMPORT_BYTES) { "El archivo supera el límite de 200 MB" }
            output.write(buffer, 0, bytesRead)
        }
    }

    private fun File.readHeader(): ByteArray =
        inputStream().use { input ->
            val header = ByteArray(IMPORT_HEADER_BYTES)
            val bytesRead = input.read(header)
            if (bytesRead <= 0) ByteArray(0) else header.copyOf(bytesRead)
        }

    private companion object {
        const val MAX_IMPORT_BYTES = 200L * 1024 * 1024
        const val COPY_BUFFER_BYTES = 64 * 1024
        const val IMPORT_HEADER_BYTES = 1_100
        const val CURRENT_DOCUMENT_NORMALIZATION_VERSION = 2
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
    val normalizationVersion: Int,
    val chapters: List<BookChapter>,
    val coverFileName: String?,
    val originalFileName: String?,
    val pageAnchors: List<BookPageAnchor>,
    val narratorAvatar: NarratorAvatar,
    val completedReadings: List<CompletedReading>,
    val currentCycleStats: ReadingCycleStats,
    val savedQuotes: List<SavedQuote>,
) {
    fun toSummary(libraryDirectory: File) =
        BookSummary(
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
            completedReadings = completedReadings,
            currentCycleStats = currentCycleStats,
            savedQuotes = savedQuotes,
        )

    fun toJson() =
        JSONObject().apply {
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
            put("normalizationVersion", normalizationVersion)
            put("coverFileName", coverFileName)
            put("originalFileName", originalFileName)
            put("narratorAvatar", narratorAvatar.name)
            put(
                "completedReadings",
                JSONArray().apply {
                    completedReadings.forEach { reading ->
                        put(
                            JSONObject().apply {
                                put("completedAtMillis", reading.completedAtMillis)
                                put("elapsedMillis", reading.elapsedMillis)
                                put("wordsRead", reading.wordsRead)
                                put("averageWordsPerMinute", reading.averageWordsPerMinute)
                                put("pauses", reading.pauses)
                                put("backwardsMoves", reading.backwardsMoves)
                                put("fragmentsRead", reading.fragmentsRead)
                            },
                        )
                    }
                },
            )
            put(
                "currentCycleStats",
                JSONObject().apply {
                    put("activeDurationMillis", currentCycleStats.activeDurationMillis)
                    put("wordsRead", currentCycleStats.wordsRead)
                    put("pauses", currentCycleStats.pauses)
                    put("backwardsMoves", currentCycleStats.backwardsMoves)
                    put("fragmentsRead", currentCycleStats.fragmentsRead)
                },
            )
            put(
                "savedQuotes",
                JSONArray().apply {
                    savedQuotes.forEach { quote ->
                        put(
                            JSONObject().apply {
                                put("id", quote.id)
                                put("bookId", quote.bookId)
                                put("bookTitle", quote.bookTitle)
                                put("chapterTitle", quote.chapterTitle)
                                put("text", quote.text)
                                put("startCharacterOffset", quote.startCharacterOffset)
                                put("endCharacterOffset", quote.endCharacterOffset)
                                put("createdAtMillis", quote.createdAtMillis)
                            },
                        )
                    }
                },
            )
            put(
                "chapters",
                JSONArray().apply {
                    chapters.forEach { chapter ->
                        put(
                            JSONObject().apply {
                                put("title", chapter.title)
                                put("startCharacterOffset", chapter.startCharacterOffset)
                            },
                        )
                    }
                },
            )
            put(
                "pageAnchors",
                JSONArray().apply {
                    pageAnchors.forEach { anchor ->
                        put(
                            JSONObject().apply {
                                put("pageIndex", anchor.pageIndex)
                                put("startCharacterOffset", anchor.startCharacterOffset)
                            },
                        )
                    }
                },
            )
        }

    companion object {
        fun fromJson(json: JSONObject): BookRecord {
            val chapterArray = json.optJSONArray("chapters") ?: JSONArray()
            val completedReadingsArray = json.optJSONArray("completedReadings") ?: JSONArray()
            val currentCycleStatsJson = json.optJSONObject("currentCycleStats")
            val savedQuotesArray = json.optJSONArray("savedQuotes") ?: JSONArray()
            val pageAnchorsArray = json.optJSONArray("pageAnchors") ?: JSONArray()
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
                contentFileName = requireSafeRecordFileName(json.getString("contentFileName")),
                normalizationVersion = json.optInt("normalizationVersion", 0),
                coverFileName =
                    json
                        .optString("coverFileName")
                        .takeIf { it.isNotBlank() && it != "null" }
                        ?.let(::requireSafeRecordFileName),
                originalFileName =
                    json
                        .optString("originalFileName")
                        .takeIf { it.isNotBlank() && it != "null" }
                        ?.let(::requireSafeRecordFileName),
                pageAnchors =
                    (0 until pageAnchorsArray.length()).map { index ->
                        pageAnchorsArray.getJSONObject(index).let { anchor ->
                            BookPageAnchor(
                                pageIndex = anchor.getInt("pageIndex"),
                                startCharacterOffset = anchor.getInt("startCharacterOffset"),
                            )
                        }
                    },
                narratorAvatar =
                    runCatching {
                        NarratorAvatar.valueOf(json.optString("narratorAvatar"))
                    }.getOrDefault(NarratorAvatar.OCTI),
                completedReadings =
                    (0 until completedReadingsArray.length()).map { index ->
                        completedReadingsArray.getJSONObject(index).let { reading ->
                            CompletedReading(
                                completedAtMillis = reading.optLong("completedAtMillis", 0),
                                elapsedMillis = reading.optLong("elapsedMillis", 0),
                                wordsRead = reading.optInt("wordsRead", 0),
                                averageWordsPerMinute = reading.optInt("averageWordsPerMinute", 0),
                                pauses = reading.optInt("pauses", 0),
                                backwardsMoves = reading.optInt("backwardsMoves", 0),
                                fragmentsRead = reading.optInt("fragmentsRead", 0),
                            )
                        }
                    },
                currentCycleStats =
                    ReadingCycleStats(
                        activeDurationMillis = currentCycleStatsJson?.optLong("activeDurationMillis", 0) ?: 0,
                        wordsRead = currentCycleStatsJson?.optInt("wordsRead", 0) ?: 0,
                        pauses = currentCycleStatsJson?.optInt("pauses", 0) ?: 0,
                        backwardsMoves = currentCycleStatsJson?.optInt("backwardsMoves", 0) ?: 0,
                        fragmentsRead = currentCycleStatsJson?.optInt("fragmentsRead", 0) ?: 0,
                    ),
                savedQuotes =
                    (0 until savedQuotesArray.length()).map { index ->
                        savedQuotesArray.getJSONObject(index).let { quote ->
                            SavedQuote(
                                id = quote.getString("id"),
                                bookId = quote.optString("bookId", json.getString("id")),
                                bookTitle = quote.optString("bookTitle", json.getString("title")),
                                chapterTitle =
                                    quote
                                        .optString("chapterTitle")
                                        .takeIf { it.isNotBlank() && it != "null" },
                                text = quote.getString("text"),
                                startCharacterOffset = quote.getInt("startCharacterOffset"),
                                endCharacterOffset = quote.getInt("endCharacterOffset"),
                                createdAtMillis = quote.optLong("createdAtMillis", 0),
                            )
                        }
                    },
                chapters =
                    (0 until chapterArray.length()).map { index ->
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

private fun requireSafeRecordFileName(value: String): String {
    require(
        value.matches(Regex("^[A-Za-z0-9._-]+$")) && value != "." && value != ".." && File(value).name == value,
    ) {
        "La biblioteca contiene una ruta de archivo no válida"
    }
    return value
}
