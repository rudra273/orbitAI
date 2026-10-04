package com.example.orbitai.feature.backup

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class NotABackupException : IOException("This isn't an Orbit backup file.")

class WrongPassphraseException : IOException("Wrong passphrase, or the backup file is damaged.")

/**
 * `.orbitbackup` container: magic, salt, IV, then AES-256-GCM ciphertext of the zip payload. The
 * key comes from the passphrase with PBKDF2-HMAC-SHA256.
 */
object BackupCodec {

    private val MAGIC = "ORBITBK1".toByteArray()
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val ITERATIONS = 210_000
    private const val TAG_BITS = 128

    private fun key(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        try {
            return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    /** Wraps [out]; close the returned stream to write the authentication tag. */
    fun encryptTo(out: OutputStream, passphrase: CharArray, iterations: Int = ITERATIONS): OutputStream {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        out.write(MAGIC)
        out.write(salt)
        out.write(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(passphrase, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        return CipherOutputStream(out, cipher)
    }

    /**
     * Decrypts into [out]. Throws [NotABackupException] for other files and [WrongPassphraseException]
     * when authentication fails, so callers should write to a temporary file and keep it only on success.
     */
    fun decrypt(input: InputStream, out: OutputStream, passphrase: CharArray, iterations: Int = ITERATIONS) {
        val header = ByteArray(MAGIC.size + SALT_BYTES + IV_BYTES)
        var read = 0
        while (read < header.size) {
            val n = input.read(header, read, header.size - read)
            if (n < 0) throw NotABackupException()
            read += n
        }
        if (!header.copyOf(MAGIC.size).contentEquals(MAGIC)) throw NotABackupException()
        val salt = header.copyOfRange(MAGIC.size, MAGIC.size + SALT_BYTES)
        val iv = header.copyOfRange(MAGIC.size + SALT_BYTES, header.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(passphrase, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        try {
            CipherInputStream(input, cipher).use { it.copyTo(out) }
        } catch (e: IOException) {
            throw WrongPassphraseException()
        }
    }
}
