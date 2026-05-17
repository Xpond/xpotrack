package com.xpotrack.app.data.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.SecretKeyFactory

object VaultCrypto {

    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val KDF_ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    const val SALT_BYTES = 16
    const val IV_BYTES = 12

    private val rng = SecureRandom()

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { rng.nextBytes(it) }

    fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(passphrase, salt, KDF_ITERATIONS, KEY_BITS)
        val raw = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(raw, "AES")
    }

    // Verifier = PBKDF2(passphrase, salt) truncated/used as-is. We compare a fresh
    // derivation against the stored bytes to validate the passphrase without holding it.
    fun deriveVerifier(passphrase: CharArray, salt: ByteArray): ByteArray =
        deriveKey(passphrase, salt).encoded

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    // Encrypts plaintext under a per-note key derived from the vault key + note salt.
    // Output layout: [salt(16) | iv(12) | ciphertext+tag].
    fun encryptNote(vaultKey: SecretKey, plaintext: ByteArray): ByteArray {
        val salt = randomBytes(SALT_BYTES)
        val noteKey = stretchWithSalt(vaultKey, salt)
        val iv = randomBytes(IV_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, noteKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ct = cipher.doFinal(plaintext)
        return salt + iv + ct
    }

    fun decryptNote(vaultKey: SecretKey, blob: ByteArray): ByteArray {
        require(blob.size > SALT_BYTES + IV_BYTES) { "vault blob too short" }
        val salt = blob.copyOfRange(0, SALT_BYTES)
        val iv = blob.copyOfRange(SALT_BYTES, SALT_BYTES + IV_BYTES)
        val ct = blob.copyOfRange(SALT_BYTES + IV_BYTES, blob.size)
        val noteKey = stretchWithSalt(vaultKey, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, noteKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    // HKDF-lite: per-note salt expands the vault key into a unique AES key per note.
    // Single-pass PBKDF2 with low iteration count — input is already a 256-bit key,
    // not a low-entropy password, so we only need domain separation.
    private fun stretchWithSalt(vaultKey: SecretKey, salt: ByteArray): SecretKey {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(vaultKey.encoded, "HmacSHA256"))
        return SecretKeySpec(mac.doFinal(salt), "AES")
    }
}
