package com.octomind.booksreader.data

import com.octomind.booksreader.domain.BookFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class BookImportFormatDetectorTest {
    @Test
    fun `detects pdf by signature when provider reports a generic type`() {
        val detected =
            BookImportFormatDetector.detect(
                displayName = "documento",
                mimeType = "application/octet-stream",
                header = "%PDF-1.7\n".toByteArray(),
            )

        assertEquals(BookFormat.PDF, detected)
    }

    @Test
    fun `detects pdf signature after a tolerated header prefix`() {
        val detected =
            BookImportFormatDetector.detect(
                displayName = "descarga.bin",
                mimeType = null,
                header = "  \n%PDF-1.4".toByteArray(),
            )

        assertEquals(BookFormat.PDF, detected)
    }

    @Test
    fun `keeps supported extension when provider reports an unknown vendor type`() {
        val detected =
            BookImportFormatDetector.detect(
                displayName = "lectura.PDF",
                mimeType = "application/x-download",
                header = ByteArray(0),
            )

        assertEquals(BookFormat.PDF, detected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a document without supported evidence`() {
        BookImportFormatDetector.detect(
            displayName = "imagen.jpg",
            mimeType = "image/jpeg",
            header = byteArrayOf(1, 2, 3),
        )
    }
}
