package com.xpotrack.app.ui.notes

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens

// Bottom format strip. Mutates a TextFieldValue by inserting markdown marks at
// the cursor/selection. Line-prefix marks (heading, list, quote, checkbox)
// operate on the line containing the caret; wrap marks (bold, italic) wrap the
// selection or insert empty wrappers at the caret.
@Composable
fun NotesFormatBar(value: TextFieldValue, onChange: (TextFieldValue) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .border(0.5.dp, XpTokens.Hair, RoundedCornerShape(0.dp))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Btn(R.drawable.ic_format_h1)     { onChange(linePrefix(value, "# ")) }
        Btn(R.drawable.ic_format_h2)     { onChange(linePrefix(value, "## ")) }
        Btn(R.drawable.ic_format_bold)   { onChange(wrap(value, "**")) }
        Btn(R.drawable.ic_format_italic) { onChange(wrap(value, "*")) }
        Btn(R.drawable.ic_format_list)   { onChange(linePrefix(value, "- ")) }
        Btn(R.drawable.ic_format_check)  { onChange(linePrefix(value, "- [ ] ")) }
        Btn(R.drawable.ic_format_quote)  { onChange(linePrefix(value, "> ")) }
        Btn(R.drawable.ic_format_code)   { onChange(wrap(value, "`")) }
    }
}

@Composable
private fun Btn(iconRes: Int, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), null, tint = XpTokens.Ink2, modifier = Modifier.size(20.dp))
    }
}

// Toggle a line prefix on the line containing the caret. If the line already
// starts with this prefix, strip it; otherwise prepend. Caret stays on the
// same line, shifted by the prefix delta.
private fun linePrefix(v: TextFieldValue, prefix: String): TextFieldValue {
    val text = v.text
    val caret = v.selection.start.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', (caret - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', caret).let { if (it < 0) text.length else it }
    val line = text.substring(lineStart, lineEnd)
    val (newLine, delta) = if (line.startsWith(prefix)) {
        line.removePrefix(prefix) to -prefix.length
    } else {
        // If line has a competing prefix from this set, replace it.
        val stripped = stripAnyKnownPrefix(line)
        (prefix + stripped) to (prefix.length + (stripped.length - line.length))
    }
    val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
    val newCaret = (caret + delta).coerceIn(lineStart, lineStart + newLine.length)
    return v.copy(text = newText, selection = TextRange(newCaret))
}

private fun stripAnyKnownPrefix(line: String): String {
    val prefixes = listOf("# ", "## ", "- [ ] ", "- [x] ", "- ", "> ")
    for (p in prefixes) if (line.startsWith(p)) return line.removePrefix(p)
    return line
}

// Wrap selection with markers. If nothing is selected, insert empty wrappers
// and place caret between them.
private fun wrap(v: TextFieldValue, mark: String): TextFieldValue {
    val text = v.text
    val sel = v.selection
    val start = sel.min; val end = sel.max
    return if (start == end) {
        val newText = text.substring(0, start) + mark + mark + text.substring(start)
        v.copy(text = newText, selection = TextRange(start + mark.length))
    } else {
        val selected = text.substring(start, end)
        val newText = text.substring(0, start) + mark + selected + mark + text.substring(end)
        v.copy(text = newText, selection = TextRange(start + mark.length, end + mark.length))
    }
}
