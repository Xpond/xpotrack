package com.xpotrack.app.ui.vault

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.GeistMono
import com.xpotrack.app.ui.theme.XpTokens
import kotlinx.coroutines.launch

@Composable
fun LockedNoteScreen(vm: VaultViewModel, noteId: Long, onBack: () -> Unit) {
    var loaded by remember(noteId) { mutableStateOf(false) }
    var title by remember(noteId) { mutableStateOf("") }
    var body by remember(noteId) { mutableStateOf("") }
    val category = "Vault"  // category picker deferred per milestone 11
    val titleFocus = remember(noteId) { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(noteId) {
        if (noteId == 0L) { loaded = true; return@LaunchedEffect }
        val existing = vm.loadNote(noteId)
        if (existing != null) { title = existing.title; body = existing.body }
        loaded = true
    }

    LaunchedEffect(loaded) {
        if (loaded && noteId == 0L) titleFocus.requestFocus()
    }

    val saveAndBack: () -> Unit = {
        scope.launch {
            val trimmed = title.trim()
            val hasContent = trimmed.isNotEmpty() || body.isNotEmpty()
            if (noteId == 0L && !hasContent) {
                // Empty new note — discard.
            } else if (noteId > 0L && !hasContent) {
                vm.deleteNote(noteId)
            } else {
                vm.saveNote(LockedNote(
                    id = noteId, title = trimmed.ifEmpty { "Untitled" }, category = category,
                    body = body, updatedAt = System.currentTimeMillis(),
                ))
            }
            onBack()
        }
    }
    BackHandler(onBack = saveAndBack)

    Column(Modifier.fillMaxSize().background(XpTokens.Bg)) {
        TopBar()
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(8.dp))
            Text(
                "$category${if (noteId == 0L) "" else " · Updated just now"}",
                style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3,
            )
            Spacer(Modifier.height(14.dp))
            TitleField(title, { title = it }, titleFocus)
            Spacer(Modifier.height(22.dp))
            BodyField(body, { body = it })
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun TopBar() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            Modifier.clip(CircleShape).background(XpTokens.TealTint)
                .border(0.5.dp, XpTokens.Hair2, CircleShape)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_lock), null, tint = XpTokens.TealDim, modifier = Modifier.size(10.dp))
            Spacer(Modifier.size(6.dp))
            Text("LOCKED", fontSize = 11.sp, color = XpTokens.TealDim,
                fontFamily = GeistMono, fontWeight = FontWeight.Medium)
        }
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
        textStyle = LocalTextStyle.current.copy(
            fontSize = 14.5.sp, lineHeight = 24.sp, color = XpTokens.Ink,
            fontFamily = GeistMono,
        ),
        cursorBrush = SolidColor(XpTokens.Teal),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(
                "Account numbers, passwords, codes. Encrypted with your vault key.",
                fontSize = 14.sp, color = XpTokens.Ink4, fontFamily = GeistMono,
            )
            inner()
        },
    )
}

