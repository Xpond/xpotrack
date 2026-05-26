package com.xpotrack.app.ui.quick

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.components.cutoutSafeTopPadding
import com.xpotrack.app.ui.notes.CaretScrollEffect
import com.xpotrack.app.ui.notes.rememberCaretScroll
import com.xpotrack.app.ui.notes.swallowBringIntoView
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun QuickEditorScreen(
    vm: QuickNotesViewModel,
    noteId: Long,
    onBack: () -> Unit,
) {
    var tfv by remember { mutableStateOf(TextFieldValue("")) }
    var initial by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    LaunchedEffect(noteId) {
        if (noteId != 0L) {
            val existing = vm.getText(noteId)
            tfv = TextFieldValue(existing, TextRange(existing.length))
            initial = existing
        } else {
            focus.requestFocus()
        }
    }

    val saveAndBack: () -> Unit = {
        val trimmed = tfv.text.trim()
        when {
            noteId == 0L && trimmed.isNotEmpty() -> vm.add(trimmed)
            noteId != 0L && trimmed != initial.trim() -> {
                if (trimmed.isEmpty()) vm.delete(noteId) else vm.update(noteId, trimmed)
            }
        }
        onBack()
    }
    BackHandler(onBack = saveAndBack)

    val bodyScroll = rememberScrollState()
    val caret = rememberCaretScroll(bodyScroll)
    CaretScrollEffect(caret, caretOffset = tfv.selection.start)

    Column(Modifier.fillMaxSize().background(XpTokens.Bg).cutoutSafeTopPadding().imePadding()) {
        Column(
            Modifier.fillMaxSize()
                .onGloballyPositioned { caret.viewportHeightPx = it.size.height }
                .verticalScroll(bodyScroll)
                .padding(horizontal = 24.dp),
        ) {
            Text(
                "Quick · disappears in 24h",
                style = MaterialTheme.typography.labelMedium,
                color = XpTokens.Ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            BasicTextField(
                value = tfv,
                onValueChange = { tfv = it },
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 26.sp, color = XpTokens.BodyInk),
                cursorBrush = SolidColor(XpTokens.Teal),
                onTextLayout = { caret.layout = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    .swallowBringIntoView()
                    .onGloballyPositioned { caret.fieldTopInScrollPx = it.positionInParent().y.toInt() },
                decorationBox = { inner ->
                    if (tfv.text.isEmpty()) Text(
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

