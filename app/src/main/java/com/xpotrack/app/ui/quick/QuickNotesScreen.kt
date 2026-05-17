package com.xpotrack.app.ui.quick

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun QuickNotesScreen(
    vm: QuickNotesViewModel,
    onBack: () -> Unit,
) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    val saved by vm.justSaved.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    Box(
        Modifier
            .fillMaxSize()
            .background(XpTokens.Bg),
    ) {
        Column(Modifier.fillMaxSize()) {
            Header(onBack = onBack)
            ComposeRow(onSave = vm::add)
            SectionMeta(count = rows.size, oldest = rows.lastOrNull()?.leftLabel, onClearAll = { confirmClear = true })
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                rows.forEachIndexed { i, r ->
                    QuickNoteEntry(row = r, isLast = i == rows.size - 1, onKeep = { vm.keep(r.id) })
                }
                Spacer(Modifier.height(16.dp))
            }
            BottomStrip()
        }
        saved?.let { QuickSavedDialog(d = it, onGotIt = vm::dismissDialog, onMove = vm::keepThenDismiss) }
        if (confirmClear) ClearAllDialog(onConfirm = { vm.clearAll(); confirmClear = false }, onDismiss = { confirmClear = false })
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 18.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_lightning),
                    contentDescription = null,
                    tint = XpTokens.TealDim,
                    modifier = Modifier.size(11.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Disappearing · 24h".uppercase(), style = MaterialTheme.typography.labelSmall, color = XpTokens.TealDim)
            }
            Spacer(Modifier.height(8.dp))
            Text("Quick", style = MaterialTheme.typography.displayLarge, color = XpTokens.Ink)
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(0.5.dp, XpTokens.Hair2, CircleShape)
                .clickable(onClick = onBack),
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

@Composable
private fun ComposeRow(onSave: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(expanded) { if (expanded) focus.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(0.5.dp, XpTokens.Teal, RoundedCornerShape(14.dp))
            .clickable(enabled = !expanded) { expanded = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (!expanded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BoltCircle()
                Spacer(Modifier.width(10.dp))
                Text(
                    "Jot something — gone tomorrow",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = XpTokens.Ink3,
                    modifier = Modifier.weight(1f),
                )
                Text("↵", style = MaterialTheme.typography.labelMedium, color = XpTokens.Ink3)
            }
        } else {
            Column {
                Row(verticalAlignment = Alignment.Top) {
                    BoltCircle()
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = LocalTextStyle.current.copy(color = XpTokens.Ink, fontSize = 14.sp, lineHeight = 20.sp),
                        cursorBrush = SolidColor(XpTokens.Teal),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                        decorationBox = { inner ->
                            if (text.isEmpty()) Text(
                                "Jot something — gone tomorrow",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = XpTokens.Ink3,
                            )
                            inner()
                        },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        color = XpTokens.Ink3,
                        modifier = Modifier.clickable { text = ""; expanded = false }.padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    val canSave = text.isNotBlank()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (canSave) XpTokens.Teal else XpTokens.Surface1)
                            .clickable(enabled = canSave) {
                                onSave(text); text = ""; expanded = false
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "Save",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            color = if (canSave) XpTokens.OnTeal else XpTokens.Ink3,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoltCircle() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0x1A5EEAD4)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = XpTokens.Teal,
            modifier = Modifier.size(13.dp),
        )
    }
}

@Composable
private fun SectionMeta(count: Int, oldest: String?, onClearAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val left = when {
            count == 0 -> "Nothing here yet"
            oldest == null -> "$count notes"
            else -> "$count notes · oldest expires in $oldest"
        }
        Text(left.uppercase(), style = MaterialTheme.typography.labelSmall, color = XpTokens.Ink3, modifier = Modifier.weight(1f))
        if (count > 0) Text(
            "Clear all".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = XpTokens.Ink3,
            modifier = Modifier.clickable(onClick = onClearAll).padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun BottomStrip() {
    Column {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(XpTokens.Hair))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(XpTokens.Surface1.copy(alpha = 0.4f))
                .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                tint = XpTokens.TealDim,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Notes here vanish 24 hours after you write them. Tap Keep to move one to your regular notes.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                color = XpTokens.Ink2,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
