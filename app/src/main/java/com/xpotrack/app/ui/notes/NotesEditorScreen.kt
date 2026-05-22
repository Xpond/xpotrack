package com.xpotrack.app.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xpotrack.app.R
import com.xpotrack.app.XpApp
import com.xpotrack.app.ui.categories.parseHexColor
import com.xpotrack.app.ui.theme.GeistMono
import com.xpotrack.app.ui.theme.XpTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesEditorScreen(vm: NotesEditorViewModel, onBack: () -> Unit, onPickCategory: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val titleFocus = remember { FocusRequester() }
    val saveAndBack: () -> Unit = { scope.launch { vm.save(); onBack() } }
    BackHandler(onBack = saveAndBack)

    // Local TextFieldValue keeps cursor/selection state; seeded once when the
    // VM finishes loading, then mirrored back via onBodyChange. Toolbar inserts
    // mutate this directly so the caret can land in the right spot.
    var bodyTfv by remember(s.loaded, s.id) { mutableStateOf(TextFieldValue(s.body, TextRange(s.body.length))) }
    val imeVisible = WindowInsets.isImeVisible

    val zoomPrefs = (LocalContext.current.applicationContext as XpApp).editorZoomPrefs
    var zoom by remember { mutableFloatStateOf(zoomPrefs.zoom) }

    LaunchedEffect(s.loaded, s.id) {
        if (s.loaded && s.id == 0 && !s.previewMode) titleFocus.requestFocus()
    }

    val bodyScroll = rememberScrollState()
    val caret = rememberCaretScroll(bodyScroll)
    CaretScrollEffect(caret, selectionKey = bodyTfv.selection)

    Column(Modifier.fillMaxSize().background(XpTokens.Bg).imePadding()) {
        TopBar(
            previewMode = s.previewMode,
            categoryName = s.categoryName,
            categoryColorHex = s.categoryColorHex,
            metaText = metaLine(s),
            onPickCategory = onPickCategory,
            onWrite = { vm.setPreview(false) },
            onPreview = { vm.setPreview(true) },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .onGloballyPositioned { caret.viewportHeightPx = it.size.height }
                .verticalScroll(bodyScroll)
                .pinchZoom(zoom) { next ->
                    zoom = next
                    zoomPrefs.zoom = next
                }
                .padding(horizontal = if (s.previewMode) 26.dp else 24.dp),
        ) {
            ZoomedText(zoom) {
                Spacer(Modifier.height(8.dp))
                if (s.previewMode) {
                    if (s.title.isNotBlank()) Heading(s.title, h1 = true)
                    MarkdownBody(s.body, onToggleTask = vm::toggleTask)
                } else {
                    TitleField(s.title, vm::onTitleChange, titleFocus)
                    Spacer(Modifier.height(12.dp))
                    BodyField(
                        value = bodyTfv,
                        onChange = { tfv ->
                            bodyTfv = tfv
                            if (tfv.text != s.body) vm.onBodyChange(tfv.text)
                        },
                        onTextLayout = { layout ->
                            caret.caretRect = layout.getCursorRect(
                                bodyTfv.selection.start.coerceIn(0, bodyTfv.text.length)
                            )
                        },
                        onFieldPositioned = { caret.fieldTopInScrollPx = it },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        if (!s.previewMode && imeVisible) {
            NotesFormatBar(bodyTfv) { tfv ->
                bodyTfv = tfv
                vm.onBodyChange(tfv.text)
            }
        }
    }
}

private fun metaLine(s: EditorState): String {
    val words = s.body.split(Regex("\\s+")).count { it.isNotBlank() }
    return if (s.id == 0 && !s.previewMode) "new note" else "$words words"
}

@Composable
private fun TopBar(
    previewMode: Boolean,
    categoryName: String,
    categoryColorHex: String?,
    metaText: String,
    onPickCategory: () -> Unit,
    onWrite: () -> Unit,
    onPreview: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryChip(categoryName, categoryColorHex, onPickCategory)
        Spacer(Modifier.width(10.dp))
        Text(metaText, color = XpTokens.Ink3, fontSize = 12.sp, letterSpacing = (-0.005).em)
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
    }
}

@Composable
private fun CategoryChip(name: String, colorHex: String?, onClick: () -> Unit) {
    val textColor = colorHex?.let { parseHexColor(it) } ?: XpTokens.Ink2
    Row(
        Modifier.clip(CircleShape).clickable(onClick = onClick)
            .background(XpTokens.Surface1)
            .border(0.5.dp, XpTokens.Hair, CircleShape)
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name, color = textColor,
            fontFamily = GeistMono, fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp, letterSpacing = 0.02.em,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            painterResource(R.drawable.ic_caret_down), null,
            tint = XpTokens.Ink3, modifier = Modifier.size(12.dp),
        )
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
private fun BodyField(
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    onTextLayout: (TextLayoutResult) -> Unit,
    onFieldPositioned: (Int) -> Unit,
) {
    BasicTextField(
        value = value, onValueChange = onChange,
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 26.sp, color = XpTokens.BodyInk),
        cursorBrush = SolidColor(XpTokens.Teal),
        onTextLayout = onTextLayout,
        modifier = Modifier.fillMaxWidth()
            .onGloballyPositioned { onFieldPositioned(it.positionInParent().y.toInt()) },
        decorationBox = { inner ->
            if (value.text.isEmpty()) Text(
                "Start writing. Markdown shortcuts work — # for heading, > for quote, ``` for code.",
                fontSize = 15.5.sp, lineHeight = 25.sp, color = XpTokens.Ink4,
            )
            inner()
        },
    )
}
