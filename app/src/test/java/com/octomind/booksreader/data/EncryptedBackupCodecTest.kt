package com.octomind.booksreader.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class EncryptedBackupCodecTest {
    @Test
    fun roundTripRestoresExactContent() {
        val expected = "biblioteca privada con progreso y citas".toByteArray()
        val encrypted = ByteArrayOutputStream()

        EncryptedBackupCodec.encrypt("contraseña-segura".toCharArray(), encrypted) { output ->
            output.write(expected)
        }

        val restored = ByteArrayOutputStream()
        EncryptedBackupCodec.decrypt(
            "contraseña-segura".toCharArray(),
            ByteArrayInputStream(encrypted.toByteArray()),
        ) { input -> input.copyTo(restored) }

        assertArrayEquals(expected, restored.toByteArray())
    }

    @Test
    fun rejectsFilesWithoutOctomindHeader() {
        try {
            EncryptedBackupCodec.decrypt(
                "contraseña-segura".toCharArray(),
                ByteArrayInputStream("archivo ajeno".toByteArray()),
            ) { it.readBytes() }
            fail("Debió rechazar un archivo sin cabecera")
        } catch (_: Exception) {
            // Resultado esperado.
        }
    }
}
