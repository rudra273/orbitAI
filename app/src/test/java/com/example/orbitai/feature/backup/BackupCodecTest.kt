package com.example.orbitai.feature.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupCodecTest {

    private val iterations = 1_000

    private fun encrypt(data: ByteArray, passphrase: String): ByteArray {
        val out = ByteArrayOutputStream()
        BackupCodec.encryptTo(out, passphrase.toCharArray(), iterations).use { it.write(data) }
        return out.toByteArray()
    }

    private fun decrypt(bytes: ByteArray, passphrase: String): ByteArray {
        val out = ByteArrayOutputStream()
        BackupCodec.decrypt(ByteArrayInputStream(bytes), out, passphrase.toCharArray(), iterations)
        return out.toByteArray()
    }

    @Test
    fun roundTripReturnsTheSameBytes() {
        val data = ByteArray(200_000) { (it * 31).toByte() }
        assertArrayEquals(data, decrypt(encrypt(data, "correct horse"), "correct horse"))
    }

    @Test
    fun ciphertextDiffersEachTimeAndHidesTheContent() {
        val data = "my secret chat".toByteArray()
        val a = encrypt(data, "passphrase1")
        assertFalse(a.decodeToString().contains("secret"))
        assertFalse(a.contentEquals(encrypt(data, "passphrase1")))
    }

    @Test
    fun wrongPassphraseIsRejected() {
        val bytes = encrypt("hello".toByteArray(), "right-pass")
        assertThrows(WrongPassphraseException::class.java) { decrypt(bytes, "wrong-pass") }
    }

    @Test
    fun tamperedFileIsRejected() {
        val bytes = encrypt(ByteArray(1000) { 7 }, "right-pass")
        bytes[bytes.size / 2] = (bytes[bytes.size / 2] + 1).toByte()
        assertThrows(WrongPassphraseException::class.java) { decrypt(bytes, "right-pass") }
    }

    @Test
    fun otherFilesAreNotBackups() {
        assertThrows(NotABackupException::class.java) { decrypt("just some text, not a backup file".toByteArray(), "x") }
        assertThrows(NotABackupException::class.java) { decrypt(ByteArray(3), "x") }
    }
}
