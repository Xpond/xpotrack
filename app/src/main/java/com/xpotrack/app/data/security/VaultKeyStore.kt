package com.xpotrack.app.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Keystore AES-GCM key gated by biometric auth. Wraps the user's vault passphrase
// bytes so that a successful BiometricPrompt unwraps them without re-typing.
//
// Flow:
//   1) initEncryptCipher() returns a Cipher in ENCRYPT_MODE — pass to BiometricPrompt.
//      On success, call wrap(cipher, passphraseBytes) → (ciphertext, iv) to persist.
//   2) initDecryptCipher(iv) returns one in DECRYPT_MODE — pass to prompt.
//      On success, call unwrap(cipher, ciphertext) → passphraseBytes.
//
// The Cipher must be auth-bound: invalidatedByBiometricEnrollment so adding/removing
// a fingerprint nukes the key (and the user falls back to passphrase).

object VaultKeyStore {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "xp_vault_bio"
    private const val GCM_TAG_BITS = 128
    private const val TRANSFORM = "AES/GCM/NoPadding"

    fun initEncryptCipher(): Cipher {
        val key = getOrCreateKey()
        return Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    fun initDecryptCipher(iv: ByteArray): Cipher {
        val key = loadKey() ?: error("biometric key missing")
        return Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
    }

    fun wrap(cipher: Cipher, plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val ct = cipher.doFinal(plaintext)
        return ct to cipher.iv
    }

    fun unwrap(cipher: Cipher, ciphertext: ByteArray): ByteArray = cipher.doFinal(ciphertext)

    private fun loadKey(): SecretKey? {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        return ks.getKey(KEY_ALIAS, null) as? SecretKey
    }

    private fun getOrCreateKey(): SecretKey {
        loadKey()?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        kg.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build()
        )
        return kg.generateKey()
    }
}
