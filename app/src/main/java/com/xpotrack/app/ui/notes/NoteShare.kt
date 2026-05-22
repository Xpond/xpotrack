package com.xpotrack.app.ui.notes

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Writes the note to cache/shared/<slug>.md and fires ACTION_SEND so the user
// can pick any share target (KDE Connect, mail, Telegram, etc). Vault notes
// must never reach this path — the caller decides eligibility.
fun shareNoteAsMarkdown(context: Context, title: String, body: String) {
    val safeTitle = title.ifBlank { "Untitled" }
    val markdown = buildString {
        append("# ").append(safeTitle).append("\n\n")
        append(body.trimEnd())
        append('\n')
    }
    val fileName = filenameFor(title) + ".md"
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    // Reuse the same filename across re-shares of the same title so the cache
    // doesn't accumulate; overwriting is fine — the URI grant is per-intent.
    val file = File(dir, fileName).apply { writeText(markdown) }
    val uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file,
    )
    val chooser = ShareCompat.IntentBuilder(context)
        .setType("text/markdown")
        .setStream(uri)
        .setSubject(safeTitle)
        .setChooserTitle("Share note")
        .createChooserIntent()
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

// Multi-note share. Writes each note to cache/shared/ then fires
// ACTION_SEND_MULTIPLE so the picker treats them as one share.
fun shareNotesAsMarkdown(context: Context, notes: List<Pair<String, String>>) {
    if (notes.size == 1) {
        val (title, body) = notes.first()
        shareNoteAsMarkdown(context, title, body)
        return
    }
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val uris = ArrayList<Uri>(notes.size)
    notes.forEachIndexed { i, (title, body) ->
        val safe = title.ifBlank { "Untitled" }
        val md = "# $safe\n\n" + body.trimEnd() + "\n"
        // Disambiguate same-titled notes with an index suffix.
        val base = filenameFor(title)
        val file = File(dir, "$base-${i + 1}.md").apply { writeText(md) }
        uris += FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
    }
    val chooser = ShareCompat.IntentBuilder(context)
        .setType("text/markdown")
        .also { b -> uris.forEach { b.addStream(it) } }
        .setChooserTitle("Share ${notes.size} notes")
        .createChooserIntent()
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

private fun filenameFor(title: String): String {
    val slug = title.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(60)
    if (slug.isNotEmpty()) return slug
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
    return "note-$today"
}
