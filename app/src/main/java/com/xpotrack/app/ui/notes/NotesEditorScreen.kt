package com.xpotrack.app.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.GeistMono
import com.xpotrack.app.ui.theme.XpTokens
import kotlinx.coroutines.launch

@Composable
fun NotesEditorScreen(vm: NotesEditorViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val titleFocus = remember { FocusRequester() }
    val saveAndBack: () -> Unit = { scope.launch { vm.save(); onBack() } }
    BackHandler(onBack = saveAndBack)
    LaunchedEffect(s.loaded, s.id) {
        if (s.loaded && s.id == 0 && !s.previewMode) titleFocus.requestFocus()
    }

    Column(Modifier.fillMaxSize().background(XpTokens.Bg)) {
        TopBar(s.previewMode, saveAndBack, { vm.setPreview(false) }, { vm.setPreview(true) })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (s.previewMode) 26.dp else 24.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(metaLine(s), style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
            Spacer(Modifier.height(if (s.previewMode) 14.dp else 16.dp))
            if (s.previewMode) {
                if (s.title.isNotBlank()) {
                    Text(
                        s.title, color = XpTokens.Ink, fontWeight = FontWeight.SemiBold,
                        fontSize = 30.sp, lineHeight = 33.sp, letterSpacing = (-0.025).em,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(36.dp).height(2.dp).background(XpTokens.Teal))
                    Spacer(Modifier.height(22.dp))
                }
                MarkdownBody(s.body)
            } else {
                TitleField(s.title, vm::onTitleChange, titleFocus)
                Spacer(Modifier.height(12.dp))
                BodyField(s.body, vm::onBodyChange)
                if (s.title.isBlank() && s.body.isBlank()) {
                    Spacer(Modifier.height(32.dp))
                    QuickMarksCard()
                }
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}

private fun metaLine(s: EditorState): String {
    val words = s.body.split(Regex("\\s+")).count { it.isNotBlank() }
    return when {
        s.previewMode -> "${s.category} · $words words"
        s.id == 0 -> "New note · just now"
        else -> "${s.category} · $words words"
    }
}

@Composable
private fun TopBar(previewMode: Boolean, onBack: () -> Unit, onWrite: () -> Unit, onPreview: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconChip(R.drawable.ic_chevron_left, "Back", onBack)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.clip(CircleShape).background(XpTokens.Surface1)
                .border(0.5.dp, XpTokens.Hair, CircleShape).padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Segment("Write", !previewMode, onWrite)
            Segment("Preview", previewMode, onPreview)
        }
        Spacer(Modifier.width(8.dp))
        IconChip(R.drawable.ic_dots_vertical, "More") {}
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (selected) XpTokens.Teal else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            label, fontFamily = GeistMono, fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp, letterSpacing = 0.02.em,
            color = if (selected) XpTokens.OnTeal else XpTokens.Ink3,
        )
    }
}

@Composable
private fun IconChip(iconRes: Int, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), desc, tint = XpTokens.Ink2, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TitleField(value: String, onChange: (String) -> Unit, focus: FocusRequester) {
    BasicTextField(
        value = value, onValueChange = onChange,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.7).sp, color = XpTokens.Ink,
        ),
        cursorBrush = SolidColor(XpTokens.Teal),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(
                "Title", fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.7).sp, color = XpTokens.Ink3,
            )
            inner()
        },
    )
}

@Composable
private fun BodyField(value: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value, onValueChange = onChange,
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 26.sp, color = XpTokens.Ink),
        cursorBrush = SolidColor(XpTokens.Teal),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(
                "Start writing. Markdown shortcuts work — # for heading, > for quote, ``` for code.",
                fontSize = 15.5.sp, lineHeight = 25.sp, color = XpTokens.Ink4,
            )
            inner()
        },
    )
}

@Composable
private fun QuickMarksCard() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color(0x0A5EEAD4))
            .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("QUICK MARKS", style = MaterialTheme.typography.labelSmall, color = XpTokens.TealDim)
        Spacer(Modifier.height(4.dp))
        QuickMarkRow("#  ", "Heading")
        QuickMarkRow("-  ", "List item")
        QuickMarkRow(">  ", "Quote")
        QuickMarkRow("```", "Code block")
        QuickMarkRow("[ ]", "Task checkbox")
    }
}

@Composable
private fun QuickMarkRow(key: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(44.dp).clip(RoundedCornerShape(6.dp)).background(XpTokens.Surface1)
                .border(0.5.dp, XpTokens.Hair, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                key,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp, letterSpacing = 0.sp),
                color = XpTokens.Teal,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp), color = XpTokens.Ink2)
    }
}
