package com.xpotrack.app.ui.quick

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun QuickEditorScreen(vm: QuickNotesViewModel, noteId: Long, onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var initial by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    LaunchedEffect(noteId) {
        if (noteId != 0L) {
            val existing = vm.getText(noteId)
            text = existing
            initial = existing
        } else {
            focus.requestFocus()
        }
    }

    val saveAndBack: () -> Unit = {
        val trimmed = text.trim()
        when {
            noteId == 0L && trimmed.isNotEmpty() -> vm.add(trimmed)
            noteId != 0L && trimmed != initial.trim() -> {
                if (trimmed.isEmpty()) vm.delete(noteId) else vm.update(noteId, trimmed)
            }
        }
        onBack()
    }
    BackHandler(onBack = saveAndBack)

    Column(Modifier.fillMaxSize().background(XpTokens.Bg)) {
        TopBar(onBack = saveAndBack)
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Quick · disappears in 24h",
                style = MaterialTheme.typography.labelMedium,
                color = XpTokens.Ink3,
            )
            Spacer(Modifier.height(16.dp))
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 26.sp, color = XpTokens.Ink),
                cursorBrush = SolidColor(XpTokens.Teal),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text(
                        "Jot something — gone tomorrow. First line becomes the title.",
                        fontSize = 15.5.sp, lineHeight = 25.sp, color = XpTokens.Ink4,
                    )
                    inner()
                },
            )
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = "Back",
                tint = XpTokens.Ink2,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
