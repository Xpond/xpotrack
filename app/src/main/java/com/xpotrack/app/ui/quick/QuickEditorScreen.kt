package com.xpotrack.app.ui.quick

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun QuickEditorScreen(
    vm: QuickNotesViewModel,
    noteId: Long,
    onBack: () -> Unit,
) {
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
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Quick · disappears in 24h",
                style = MaterialTheme.typography.labelMedium,
                color = XpTokens.Ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
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

