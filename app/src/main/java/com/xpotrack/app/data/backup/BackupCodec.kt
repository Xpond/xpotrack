package com.xpotrack.app.data.backup

import com.xpotrack.app.data.security.VaultCrypto
import org.json.JSONObject
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

// Backup file layout:
//   magic(4) | version(1) | salt(16) | iv(12) | ciphertext+tag
//
// Plaintext (inside the AES-GCM envelope) is a length-prefixed bundle:
//   [u32 manifestLen][manifest JSON utf-8]
//   [u32 dbLen][db bytes]
//   [u32 secureLen][secure prefs JSON utf-8]   # vault salt, verifier, db passphrase
//   [u32 plainLen][plain prefs JSON utf-8]     # theme, editor zoom
//
// Biometric blob is intentionally omitted — Keystore-bound, can't survive a restore.
// Vault unlock falls back to passphrase on the restored device.

object BackupCodec {

    val MAGIC = byteArrayOf('X'.code.toByte(), 'P'.code.toByte(), 'B'.code.toByte(), '1'.code.toByte())
    const val VERSION: Byte = 1
    private const val GCM_TAG_BITS = 128

    data class Bundle(
        val manifest: JSONObject,
        val dbBytes: ByteArray,
        val securePrefs: JSONObject,
        val plainPrefs: JSONObject,
    )

    class BadFormat(msg: String) : Exception(msg)

    fun encode(bundle: Bundle, passphrase: CharArray): ByteArray {
        val plaintext = serialize(bundle)
        val salt = VaultCrypto.randomBytes(VaultCrypto.SALT_BYTES)
        val iv = VaultCrypto.randomBytes(VaultCrypto.IV_BYTES)
        val key = VaultCrypto.deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ct = cipher.doFinal(plaintext)
        return MAGIC + byteArrayOf(VERSION) + salt + iv + ct
    }

    private fun serialize(b: Bundle): ByteArray {
        val manifestBytes = b.manifest.toString().toByteArray(Charsets.UTF_8)
        val secureBytes = b.securePrefs.toString().toByteArray(Charsets.UTF_8)
        val plainBytes = b.plainPrefs.toString().toByteArray(Charsets.UTF_8)
        val total = 16 + manifestBytes.size + b.dbBytes.size + secureBytes.size + plainBytes.size
        val buf = ByteBuffer.allocate(total)
        buf.putInt(manifestBytes.size).put(manifestBytes)
        buf.putInt(b.dbBytes.size).put(b.dbBytes)
        buf.putInt(secureBytes.size).put(secureBytes)
        buf.putInt(plainBytes.size).put(plainBytes)
        return buf.array()
    }

    fun decode(blob: ByteArray, passphrase: CharArray): Bundle {
        if (blob.size < MAGIC.size + 1 + VaultCrypto.SALT_BYTES + VaultCrypto.IV_BYTES) {
            throw BadFormat("backup file too small")
        }
        for (i in MAGIC.indices) if (blob[i] != MAGIC[i]) throw BadFormat("not an xpotrack backup")
        val version = blob[MAGIC.size]
        if (version != VERSION) throw BadFormat("unsupported backup version: $version")
        var p = MAGIC.size + 1
        val salt = blob.copyOfRange(p, p + VaultCrypto.SALT_BYTES); p += VaultCrypto.SALT_BYTES
        val iv = blob.copyOfRange(p, p + VaultCrypto.IV_BYTES); p += VaultCrypto.IV_BYTES
        val ct = blob.copyOfRange(p, blob.size)
        val key = VaultCrypto.deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val plain = cipher.doFinal(ct)
        return parse(plain)
    }

    private fun parse(plain: ByteArray): Bundle {
        val buf = ByteBuffer.wrap(plain)
        val manifest = JSONObject(String(readChunk(buf), Charsets.UTF_8))
        val db = readChunk(buf)
        val secure = JSONObject(String(readChunk(buf), Charsets.UTF_8))
        val plainPrefs = JSONObject(String(readChunk(buf), Charsets.UTF_8))
        return Bundle(manifest, db, secure, plainPrefs)
    }

    private fun readChunk(buf: ByteBuffer): ByteArray {
        val len = buf.int
        if (len < 0 || len > buf.remaining()) throw BadFormat("corrupt chunk length: $len")
        val out = ByteArray(len)
        buf.get(out)
        return out
    }
}
