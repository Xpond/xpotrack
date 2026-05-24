package com.xpotrack.app.data.backup

import android.content.Context
import android.net.Uri
import com.xpotrack.app.data.db.XpDatabase
import com.xpotrack.app.data.security.buildEncryptedPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.Instant

private const val CURRENT_SCHEMA = 11

class BackupManager(private val appContext: Context) {

    // Returns the number of bytes written. Caller owns the SAF Uri lifecycle.
    suspend fun exportTo(uri: Uri, passphrase: CharArray): Int = withContext(Dispatchers.IO) {
        // Collapse WAL into the main DB file so a byte copy is consistent.
        XpDatabase.checkpointForBackup(appContext)
        val dbFile = appContext.getDatabasePath("xpotrack.db")
        val dbBytes = dbFile.readBytes()
        val bundle = BackupCodec.Bundle(
            manifest = manifest(),
            dbBytes = dbBytes,
            securePrefs = dumpEncryptedPrefs(),
            plainPrefs = dumpPlainPrefs(),
        )
        val blob = BackupCodec.encode(bundle, passphrase)
        appContext.contentResolver.openOutputStream(uri, "w")?.use { it.write(blob) }
            ?: error("Could not open backup destination for writing")
        blob.size
    }

    private fun manifest(): JSONObject = JSONObject().apply {
        put("schemaVersion", CURRENT_SCHEMA)
        put("appVersionCode", appVersionCode())
        put("exportedAt", Instant.now().toString())
    }

    private fun appVersionCode(): Long = try {
        val pi = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        @Suppress("DEPRECATION") pi.longVersionCode
    } catch (_: Throwable) { 0L }

    private fun dumpEncryptedPrefs(): JSONObject {
        val obj = JSONObject()
        for (file in arrayOf("xp_secure_prefs", "xp_vault_prefs")) {
            val sp = buildEncryptedPrefs(appContext, file)
            val inner = JSONObject()
            for ((k, v) in sp.all) {
                // Biometric blob is Keystore-bound — useless after restore.
                // Drop it at the source so the backup file doesn't carry dead bytes.
                if (k == "vault_bio_blob" || k == "vault_bio_iv") continue
                if (v is String) inner.put(k, v)
            }
            obj.put(file, inner)
        }
        return obj
    }

    // Reads the backup and swaps the DB + prefs in place. Returns on success;
    // caller is responsible for restarting the app from the foreground Activity
    // so background-launch restrictions don't strand us. Throws on bad magic,
    // wrong passphrase, or corrupt payload — live install untouched in those cases.
    suspend fun restoreFrom(uri: Uri, passphrase: CharArray) = withContext(Dispatchers.IO) {
        val blob = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not open backup file for reading")
        val bundle = BackupCodec.decode(blob, passphrase)

        val schema = bundle.manifest.optInt("schemaVersion", -1)
        require(schema in 1..CURRENT_SCHEMA) { "Unsupported backup schema: $schema" }

        // Stage the DB to a sibling temp file so the live .db survives any
        // failure between here and the rename.
        val dbFile = appContext.getDatabasePath("xpotrack.db")
        dbFile.parentFile?.mkdirs()
        val staged = File(dbFile.parentFile, "xpotrack.db.restore")
        staged.writeBytes(bundle.dbBytes)

        // Commit prefs first so the next launch reads the restored DB passphrase.
        applyEncryptedPrefs(bundle.securePrefs)
        applyPlainPrefs(bundle.plainPrefs)

        // Now swap the DB and clear sidecars — stale WAL would corrupt the
        // restored main file on next open.
        XpDatabase.checkpointForBackup(appContext)
        File(dbFile.parentFile, "xpotrack.db-wal").delete()
        File(dbFile.parentFile, "xpotrack.db-shm").delete()
        if (!staged.renameTo(dbFile)) {
            staged.copyTo(dbFile, overwrite = true)
            staged.delete()
        }

    }

    private fun applyEncryptedPrefs(secure: JSONObject) {
        for (file in arrayOf("xp_secure_prefs", "xp_vault_prefs")) {
            val sp = buildEncryptedPrefs(appContext, file)
            val edit = sp.edit().clear()
            val inner = secure.optJSONObject(file) ?: continue
            val keys = inner.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                edit.putString(k, inner.getString(k))
            }
            edit.commit()
        }
    }

    private fun applyPlainPrefs(plain: JSONObject) {
        plain.optJSONObject("xp_theme")?.let { obj ->
            val sp = appContext.getSharedPreferences("xp_theme", Context.MODE_PRIVATE).edit().clear()
            val it = obj.keys(); while (it.hasNext()) { val k = it.next(); sp.putBoolean(k, obj.getString(k).toBoolean()) }
            sp.commit()
        }
        plain.optJSONObject("xp_editor")?.let { obj ->
            val sp = appContext.getSharedPreferences("xp_editor", Context.MODE_PRIVATE).edit().clear()
            val it = obj.keys(); while (it.hasNext()) { val k = it.next(); sp.putFloat(k, obj.getString(k).toFloat()) }
            sp.commit()
        }
    }

    private fun dumpPlainPrefs(): JSONObject {
        val obj = JSONObject()
        val theme = appContext.getSharedPreferences("xp_theme", Context.MODE_PRIVATE)
        val editor = appContext.getSharedPreferences("xp_editor", Context.MODE_PRIVATE)
        obj.put("xp_theme", JSONObject(theme.all.mapValues { it.value?.toString() ?: "" }))
        obj.put("xp_editor", JSONObject(editor.all.mapValues { it.value?.toString() ?: "" }))
        return obj
    }
}

fun defaultBackupFilename(now: Instant = Instant.now()): String {
    val ts = now.toString().substringBefore('.').replace(':', '-')
    return "xpotrack-$ts.xpb"
}
