package com.xpotrack.app.ui.expense

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.data.model.parseExpenseLine
import com.xpotrack.app.ui.theme.GeistMono
import com.xpotrack.app.ui.theme.XpTokens

// The open day's editor: one expense per line on ruled rows. Amounts tint
// teal, lines without one dim to Ink3 so it's obvious they aren't counted.
@Composable
internal fun ExpenseDayField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    readOnly: Boolean,
    // Raise the keyboard once composed — only when the user opened this day.
    focusNow: Boolean,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(focusNow) { if (focusNow) focus.requestFocus() }
    val style = TextStyle(
        fontFamily = GeistMono, fontSize = 14.sp, lineHeight = 36.sp, color = XpTokens.BodyInk,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
    val amountInk = XpTokens.Teal
    val noteInk = XpTokens.Ink3
    val rule = XpTokens.Hair2
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        textStyle = style,
        cursorBrush = SolidColor(XpTokens.Teal),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        visualTransformation = { text ->
            TransformedText(highlight(text.text, amountInk, noteInk), OffsetMapping.Identity)
        },
        onTextLayout = { layout = it },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .drawBehind {
                val l = layout ?: return@drawBehind
                val stroke = 0.5.dp.toPx()
                for (i in 0 until l.lineCount) {
                    val y = l.getLineBottom(i) - stroke / 2
                    drawLine(rule, Offset(0f, y), Offset(size.width, y), stroke)
                }
            },
        decorationBox = { inner ->
            Box {
                if (value.text.isEmpty()) Text("Lunch 260", style = style.copy(color = XpTokens.Ink4))
                inner()
            }
        },
    )
}

private fun highlight(text: String, amount: Color, note: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    var start = 0
    for (line in text.split('\n')) {
        val parsed = parseExpenseLine(line)
        if (parsed != null) {
            addStyle(SpanStyle(color = amount), start + parsed.amountRange.first, start + parsed.amountRange.last + 1)
        } else if (line.isNotBlank()) {
            addStyle(SpanStyle(color = note), start, start + line.length)
        }
        start += line.length + 1
    }
}
