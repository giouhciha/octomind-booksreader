package com.octomind.booksreader.data

import com.octomind.booksreader.domain.BookFormat
import java.nio.charset.StandardCharsets

internal object BookImportFormatDetector {
    fun detect(
        displayName: String,
        mimeType: String?,
        header: ByteArray,
    ): BookFormat {
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val normalizedMimeType = mimeType?.substringBefore(';')?.trim()?.lowercase()

        if (isPdf(header)) {
            return BookFormat.PDF
        }

        return when {
            extension == "pdf" -> BookFormat.PDF
            extension == "epub" -> BookFormat.EPUB
            extension == "txt" -> BookFormat.TXT
            normalizedMimeType != null && normalizedMimeType in PDF_MIME_TYPES -> BookFormat.PDF
            normalizedMimeType != null && normalizedMimeType in EPUB_MIME_TYPES -> BookFormat.EPUB
            normalizedMimeType?.startsWith("text/") == true -> BookFormat.TXT
            header.startsWithSignature(ZIP_SIGNATURE) -> BookFormat.EPUB
            else -> throw IllegalArgumentException("Puedes importar archivos TXT, EPUB o PDF")
        }
    }

    fun isPdf(header: ByteArray): Boolean = header.containsSignature(PDF_SIGNATURE)

    private fun ByteArray.containsSignature(signature: ByteArray): Boolean {
        if (size < signature.size) return false
        val lastStart = minOf(size - signature.size, MAX_PDF_HEADER_OFFSET)
        return (0..lastStart).any { start ->
            signature.indices.all { offset -> this[start + offset] == signature[offset] }
        }
    }

    private fun ByteArray.startsWithSignature(signature: ByteArray): Boolean =
        size >= signature.size && signature.indices.all { index -> this[index] == signature[index] }

    private val PDF_MIME_TYPES = setOf("application/pdf", "application/x-pdf")
    private val EPUB_MIME_TYPES = setOf("application/epub+zip", "application/zip")
    private val PDF_SIGNATURE = "%PDF-".toByteArray(StandardCharsets.US_ASCII)
    private val ZIP_SIGNATURE = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4)
    private const val MAX_PDF_HEADER_OFFSET = 1_024
}
