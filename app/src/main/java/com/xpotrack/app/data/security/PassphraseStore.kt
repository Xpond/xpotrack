package com.xpotrack.app.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

// Generates the 32-byte SQLCipher passphrase on first launch and stores it inside
// EncryptedSharedPreferences (Android Keystore-backed). Subsequent launches read it back.
//
// Threat model: storage of *this app's data*, on *this device*. Key is bound to the device
// — uninstalling the app destroys the Keystore wrapping key, which renders the DB unreadable.
// That's a feature, not a bug.

class PassphraseStore(context: Context) {

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

    fun getOrCreate(): ByteArray {
        prefs.getString(KEY_PASSPHRASE, null)?.let { return Base64.decode(it, Base64.NO_WRAP) }
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_PASSPHRASE, Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
        return bytes
    }

    private companion object {
        const val PREFS_FILE = "xp_secure_prefs"
        const val KEY_PASSPHRASE = "db_passphrase_b64"
    }
}
