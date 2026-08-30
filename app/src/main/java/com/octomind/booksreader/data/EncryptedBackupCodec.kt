package com.octomind.booksreader.data

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object EncryptedBackupCodec {
    private val magic = "OCTOMIND-BACKUP".toByteArray(Charsets.US_ASCII)
    private const val FORMAT_VERSION = 1
    private const val KEY_BITS = 256
    private const val ITERATIONS = 210_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val MINIMUM_PASSWORD_LENGTH = 8

    fun encrypt(
        password: CharArray,
        output: OutputStream,
        writePlaintext: (OutputStream) -> Unit,
    ) {
        require(password.size >= MINIMUM_PASSWORD_LENGTH) { "La contraseña debe tener al menos 8 caracteres" }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val key = deriveKey(password, salt)
        try {
            val dataOutput = DataOutputStream(output)
            dataOutput.write(magic)
            dataOutput.writeInt(FORMAT_VERSION)
            dataOutput.write(salt)
            dataOutput.write(iv)
            dataOutput.flush()
            val cipher =
                Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                    updateAAD(magic)
                }
            CipherOutputStream(output, cipher).use(writePlaintext)
        } finally {
            key.encoded.fill(0)
            password.fill('\u0000')
        }
    }

    fun decrypt(
        password: CharArray,
        input: InputStream,
        readPlaintext: (InputStream) -> Unit,
    ) {
        require(password.isNotEmpty()) { "Escribe la contraseña del respaldo" }
        val dataInput = DataInputStream(input)
        val foundMagic = ByteArray(magic.size).also(dataInput::readFully)
        require(foundMagic.contentEquals(magic)) { "El archivo no es un respaldo de Octomind" }
        require(dataInput.readInt() == FORMAT_VERSION) { "Esta versión del respaldo no es compatible" }
        val salt = ByteArray(SALT_BYTES).also(dataInput::readFully)
        val iv = ByteArray(IV_BYTES).also(dataInput::readFully)
        val key = deriveKey(password, salt)
        try {
            val cipher =
                Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                    updateAAD(magic)
                }
            try {
                CipherInputStream(input, cipher).use(readPlaintext)
            } catch (_: AEADBadTagException) {
                error("La contraseña es incorrecta o el respaldo está dañado")
            }
        } finally {
            key.encoded.fill(0)
            password.fill('\u0000')
        }
    }

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
    ): SecretKeySpec {
        val specification = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        return try {
            val bytes =
                SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification)
                    .encoded
            SecretKeySpec(bytes, "AES").also { bytes.fill(0) }
        } finally {
            specification.clearPassword()
        }
    }
}
