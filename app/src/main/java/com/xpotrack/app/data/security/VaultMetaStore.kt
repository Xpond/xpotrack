package com.xpotrack.app.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Stores vault setup metadata: KDF salt + verifier hash for passphrase check,
// and an optional biometric-wrapped passphrase blob. Backed by EncryptedSharedPreferences.

class VaultMetaStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun isSetup(): Boolean = prefs.contains(KEY_SALT) && prefs.contains(KEY_VERIFIER)

    fun saveSetup(salt: ByteArray, verifier: ByteArray) {
        prefs.edit()
            .putString(KEY_SALT, b64(salt))
            .putString(KEY_VERIFIER, b64(verifier))
            .apply()
    }

    fun salt(): ByteArray = decode(KEY_SALT)
    fun verifier(): ByteArray = decode(KEY_VERIFIER)

    fun saveBiometric(wrapped: ByteArray, iv: ByteArray) {
        prefs.edit()
            .putString(KEY_BIO_BLOB, b64(wrapped))
            .putString(KEY_BIO_IV, b64(iv))
            .apply()
    }

    fun hasBiometric(): Boolean = prefs.contains(KEY_BIO_BLOB) && prefs.contains(KEY_BIO_IV)
    fun biometricBlob(): ByteArray = decode(KEY_BIO_BLOB)
    fun biometricIv(): ByteArray = decode(KEY_BIO_IV)

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun decode(k: String) = Base64.decode(prefs.getString(k, null), Base64.NO_WRAP)

    private companion object {
        const val PREFS_FILE = "xp_vault_prefs"
        const val KEY_SALT = "vault_salt"
        const val KEY_VERIFIER = "vault_verifier"
        const val KEY_BIO_BLOB = "vault_bio_blob"
        const val KEY_BIO_IV = "vault_bio_iv"
    }
}
