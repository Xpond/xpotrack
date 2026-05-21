package com.xpotrack.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.GeistMono
import com.xpotrack.app.ui.theme.XpTokens

// Subset matching markdown-preview.jsx: # / ## / - / > / ``` / paragraph + **bold** / *italic*.
// Task lines (`- [ ] ` / `- [x] `) render as tappable checkboxes; the tap index
// is the 0-based position among all task lines in the source so the VM can
// toggle the correct one.
@Composable
fun MarkdownBody(src: String, onToggleTask: (Int) -> Unit = {}) {
    val lines = src.split('\n')
    var taskCursor = 0
    Column(Modifier.fillMaxWidth()) {
        var i = 0
        while (i < lines.size) {
            val ln = lines[i]
            when {
                ln.startsWith("# ") -> { Heading(ln.removePrefix("# ").trim(), h1 = true); i++ }
                ln.startsWith("## ") -> { Heading(ln.removePrefix("## ").trim(), h1 = false); i++ }
                ln.startsWith("```") -> {
                    val buf = mutableListOf<String>(); i++
                    while (i < lines.size && !lines[i].startsWith("```")) { buf += lines[i]; i++ }
                    if (i < lines.size) i++
                    CodeBlock(buf)
                }
                ln.startsWith("> ") -> { Quote(ln.removePrefix("> ").trim()); i++ }
                isTaskLine(ln) -> {
                    val items = mutableListOf<Pair<Boolean, String>>()
                    val firstIndex = taskCursor
                    while (i < lines.size && isTaskLine(lines[i])) {
                        val checked = lines[i].startsWith("- [x] ")
                        val text = lines[i].removePrefix(if (checked) "- [x] " else "- [ ] ").trim()
                        items += checked to text
                        i++; taskCursor++
                    }
                    TaskList(items, firstIndex, onToggleTask)
                }
                ln.startsWith("- ") -> {
                    val items = mutableListOf<String>()
                    while (i < lines.size && lines[i].startsWith("- ") && !isTaskLine(lines[i])) {
                        items += lines[i].removePrefix("- ").trim(); i++
                    }
                    BulletList(items)
                }
                ln.isBlank() -> i++
                else -> {
                    val buf = StringBuilder(ln); i++
                    while (i < lines.size && lines[i].isNotBlank() && !isBlockStart(lines[i])) {
                        buf.append(' ').append(lines[i].trim()); i++
                    }
                    Paragraph(buf.toString())
                }
            }
        }
    }
}

private fun isTaskLine(s: String) = s.startsWith("- [ ] ") || s.startsWith("- [x] ")

private fun isBlockStart(s: String) =
    s.startsWith("# ") || s.startsWith("## ") || s.startsWith("- ") ||
    s.startsWith("> ") || s.startsWith("```")

@Composable
private fun TaskList(items: List<Pair<Boolean, String>>, firstIndex: Int, onToggle: (Int) -> Unit) {
    items.forEachIndexed { idx, (checked, text) ->
        Row(
            Modifier.fillMaxWidth().clickable { onToggle(firstIndex + idx) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (checked) XpTokens.Teal else Color.Transparent)
                    .border(1.dp, if (checked) XpTokens.Teal else XpTokens.Ink3, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) Icon(
                    painterResource(R.drawable.ic_check), null,
                    tint = XpTokens.OnTeal, modifier = Modifier.size(12.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                inline(text),
                color = if (checked) XpTokens.Ink3 else XpTokens.Ink,
                fontSize = 16.sp, lineHeight = 25.sp,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
        Spacer(Modifier.height(6.dp))
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
internal fun Heading(text: String, h1: Boolean) {
    Spacer(Modifier.height(if (h1) 8.dp else 20.dp))
    Text(
        text, color = XpTokens.Ink, fontWeight = FontWeight.SemiBold,
        fontSize = if (h1) 30.sp else 21.sp,
        lineHeight = if (h1) 33.sp else 26.sp,
        letterSpacing = if (h1) (-0.025).em else (-0.018).em,
    )
    if (h1) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(36.dp).height(2.dp).background(XpTokens.Teal))
        Spacer(Modifier.height(22.dp))
    } else Spacer(Modifier.height(12.dp))
}

@Composable
private fun Paragraph(text: String) {
    Text(
        inline(text), color = XpTokens.Ink,
        fontSize = 16.sp, lineHeight = 27.sp, letterSpacing = (-0.003).em,
    )
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun BulletList(items: List<String>) {
    items.forEach { item ->
        Row {
            Text("—", color = XpTokens.Teal, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 25.sp)
            Spacer(Modifier.width(12.dp))
            Text(inline(item), color = XpTokens.Ink, fontSize = 16.sp, lineHeight = 25.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Quote(text: String) {
    Spacer(Modifier.height(6.dp))
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
            .background(Color(0x0A5EEAD4)),
    ) {
        Box(Modifier.width(2.dp).background(XpTokens.Teal))
        Text(
            text, color = XpTokens.Ink, fontStyle = FontStyle.Italic,
            fontSize = 18.sp, lineHeight = 27.sp, letterSpacing = (-0.005).em,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun CodeBlock(lines: List<String>) {
    Spacer(Modifier.height(6.dp))
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(XpTokens.Surface1)
            .border(0.5.dp, XpTokens.Hair, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        lines.forEach {
            Text(it, fontFamily = GeistMono, fontSize = 13.sp, lineHeight = 22.sp, color = XpTokens.Ink)
        }
    }
    Spacer(Modifier.height(18.dp))
}

private fun inline(s: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < s.length) {
        if (i + 1 < s.length && s[i] == '*' && s[i + 1] == '*') {
            val end = s.indexOf("**", i + 2)
            if (end > 0) {
                withStyle(SpanStyle(color = XpTokens.Teal, fontWeight = FontWeight.SemiBold)) {
                    append(s.substring(i + 2, end))
                }
                i = end + 2; continue
            }
        }
        if (s[i] == '*') {
            val end = s.indexOf('*', i + 1)
            if (end > 0) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(s.substring(i + 1, end)) }
                i = end + 1; continue
            }
        }
        if (s[i] == '`') {
            val end = s.indexOf('`', i + 1)
            if (end > 0) {
                withStyle(SpanStyle(fontFamily = GeistMono, color = XpTokens.TealDim, fontSize = 14.sp)) {
                    append(s.substring(i + 1, end))
                }
                i = end + 1; continue
            }
        }
        append(s[i]); i++
    }
}
