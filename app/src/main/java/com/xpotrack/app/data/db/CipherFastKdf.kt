package com.xpotrack.app.data.db

// SQLCipher's default 256,000 PBKDF2 iterations exist to slow brute-force on
// low-entropy human passwords. Our key is 32 random bytes from Android
// Keystore — already 256 bits of entropy — so PBKDF2 adds zero practical
// security but costs ~800ms per cold open.
//
// SQLCipher recognizes "x'<64hex>'" as a pre-derived 256-bit key and skips
// PBKDF2 entirely. Passing the passphrase in this form is the documented
// fast path for apps with Keystore-managed keys.

object CipherFastKdf {

    // ASCII bytes of "x'<64hex>'". SupportOpenHelperFactory accepts a byte[]
    // and forwards it to SQLCipher's key handling, which parses this literal
    // and uses the 32 bytes directly as the encryption key.
    fun rawKeyLiteral(passphrase: ByteArray): ByteArray {
        require(passphrase.size == 32) { "raw key must be 32 bytes" }
        val sb = StringBuilder(4 + passphrase.size * 2)
        sb.append("x'")
        for (b in passphrase) sb.append(HEX[(b.toInt() ushr 4) and 0xF]).append(HEX[b.toInt() and 0xF])
        sb.append('\'')
        return sb.toString().toByteArray(Charsets.US_ASCII)
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
