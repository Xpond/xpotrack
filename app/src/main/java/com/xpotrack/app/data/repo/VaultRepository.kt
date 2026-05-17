package com.xpotrack.app.data.repo

import com.xpotrack.app.data.db.NoteDao
import com.xpotrack.app.data.db.NoteEntity
import com.xpotrack.app.data.security.VaultCrypto
import com.xpotrack.app.ui.vault.LockedNote
import com.xpotrack.app.ui.vault.LockedNoteRow
import javax.crypto.SecretKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Vault-side view of the notes table. Locked rows store title + category plaintext
// (so the list can render previews) and the body encrypted in encryptedBlob.
//
// The vault key is held by VaultSession while unlocked; we require it as an arg
// rather than reaching into the session, so the repo stays pure and unit-testable.

class VaultRepository(private val dao: NoteDao) {

    fun observeLocked(): Flow<List<LockedNoteRow>> =
        dao.observeLocked().map { rows -> rows.map(::toRow) }

    suspend fun open(id: Long, key: SecretKey): LockedNote? {
        val e = dao.getById(id) ?: return null
        if (!e.isLocked) return null
        val body = e.encryptedBlob?.let { String(VaultCrypto.decryptNote(key, it)) } ?: ""
        return LockedNote(
            id = e.id,
            title = e.title,
            category = e.category,
            body = body,
            updatedAt = e.updatedAt,
        )
    }

    suspend fun upsert(note: LockedNote, key: SecretKey): Long {
        val now = System.currentTimeMillis()
        val existing = if (note.id > 0) dao.getById(note.id) else null
        val blob = VaultCrypto.encryptNote(key, note.body.toByteArray())
        val entity = NoteEntity(
            id = note.id,
            title = note.title,
            bodyMarkdown = "",
            category = note.category,
            isPinned = false,
            isLocked = true,
            encryptedBlob = blob,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        return dao.upsert(entity)
    }

    suspend fun delete(id: Long) = dao.delete(id)

    private fun toRow(e: NoteEntity): LockedNoteRow = LockedNoteRow(
        id = e.id,
        title = e.title,
        category = e.category,
        when_ = formatWhen(e.updatedAt),
    )
}
