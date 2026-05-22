package com.xpotrack.app.ui.alarm

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.GeistMono
import com.xpotrack.app.ui.theme.XpTokens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.delay

data class NoteSnippet(val body: String, val updatedAt: Long, val fromLinked: Boolean)

private val zone: ZoneId = ZoneId.systemDefault()
private val hmFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
private val mono12 = TextStyle(fontFamily = GeistMono, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.04.em)
private val monoCaps10 = TextStyle(fontFamily = GeistMono, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.12.em)

@Composable
fun AlarmTopRow(time: String) {
    val display = remember(time) {
        val t = parseHm(time) ?: LocalTime.now(zone)
        "${t.format(hmFmt)} · ${LocalDate.now(zone).format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))}"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(XpTokens.Alarm))
        Spacer(Modifier.width(10.dp))
        Text("ALARM", color = XpTokens.Alarm, style = mono12.copy(fontSize = 11.sp, letterSpacing = 0.14.em))
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f).height(1.dp).background(XpTokens.Hair))
        Spacer(Modifier.width(14.dp))
        Text(display, color = XpTokens.Ink3, style = mono12)
    }
}

@Composable
fun AlarmTitleBlock(title: String, repeat: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            color = XpTokens.Ink,
            style = TextStyle(fontSize = 50.sp, fontWeight = FontWeight.Light, lineHeight = 54.sp, letterSpacing = (-0.01).em),
        )
        if (repeat != "none") {
            Spacer(Modifier.height(12.dp))
            Text("recurs ${recurLabel(repeat)}", color = XpTokens.Ink2, style = mono12.copy(fontSize = 13.sp))
        }
    }
}

@Composable
fun NoteCard(note: NoteSnippet) {
    val header = remember(note) {
        val src = if (note.fromLinked) "FROM YOUR NOTE" else "TASK NOTES"
        if (note.fromLinked) "$src · ${ageLabel(note.updatedAt)}" else src
    }
    val snippet = remember(note.body) { snippetOf(note.body) }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.025f))
            .drawBehind {
                val w = 2.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to XpTokens.Teal.copy(alpha = 0f),
                        0.5f to XpTokens.Teal.copy(alpha = 0.35f),
                        1f to XpTokens.Teal.copy(alpha = 0f),
                    ),
                    topLeft = Offset.Zero,
                    size = Size(w, size.height),
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column {
            Text(header, color = XpTokens.Ink3, style = monoCaps10)
            Spacer(Modifier.height(8.dp))
            Text(
                "“$snippet”",
                color = XpTokens.BodyInk,
                style = TextStyle(fontSize = 14.sp, fontStyle = FontStyle.Italic, lineHeight = 20.sp),
            )
        }
    }
}

@Composable
fun SnoozeList(onSnooze: (Int) -> Unit, baseTime: String) {
    val options = remember(baseTime) {
        val now = LocalTime.now(zone)
        listOf("5 minutes" to 5, "15 minutes" to 15, "1 hour" to 60, "3 hours" to 180)
            .map { (label, mins) -> Triple(label, mins, now.plusMinutes(mins.toLong())) }
    }
    Column(Modifier.fillMaxWidth()) {
        Text("SNOOZE UNTIL", color = XpTokens.Ink3, style = monoCaps10)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { (label, mins, target) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.035f))
                        .clickable { onSnooze(mins) }
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, color = XpTokens.Ink, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
                    Text(target.format(hmFmt), color = XpTokens.Teal, style = TextStyle(fontFamily = GeistMono, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.04.em))
                }
            }
        }
    }
}

private const val HOLD_MS = 400

@Composable
fun HoldToMarkDone(onDone: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    var fired by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(if (pressed) HOLD_MS else 180, easing = LinearEasing),
        label = "hold",
    )
    LaunchedEffect(pressed) {
        if (pressed && !fired) {
            delay(HOLD_MS.toLong())
            if (pressed) { fired = true; onDone() }
        }
    }
    Box(
        Modifier
            .width(260.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    try { tryAwaitRelease() } finally { if (!fired) pressed = false }
                })
            },
    ) {
        Text(
            "Hold to mark done",
            color = XpTokens.Ink,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            modifier = Modifier.align(Alignment.Center),
        )
        HoldKnob(progress, Modifier.align(Alignment.CenterStart).padding(start = 5.dp))
    }
}

@Composable
private fun HoldKnob(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(50.dp)
            .drawBehind {
                val stroke = 2.dp.toPx()
                val inset = stroke / 2f
                drawCircle(XpTokens.Teal.copy(alpha = 0.22f), (size.minDimension - stroke) / 2f, style = Stroke(stroke))
                drawArc(
                    color = XpTokens.Teal,
                    startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(XpTokens.Teal), contentAlignment = Alignment.Center) {
            Text("✓", color = XpTokens.OnTeal, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun parseHm(s: String): LocalTime? = runCatching {
    val (h, m) = s.split(":").map { it.toInt() }
    LocalTime.of(h, m)
}.getOrNull()

private fun recurLabel(rule: String) = when (rule) {
    "weekdays" -> "on weekdays"
    else -> rule
}

private fun ageLabel(updatedAt: Long): String {
    val days = ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(updatedAt).atZone(zone).toLocalDate(),
        LocalDate.now(zone),
    ).toInt()
    return when {
        days <= 0 -> "TODAY"
        days == 1 -> "YESTERDAY"
        else -> "$days DAYS AGO"
    }
}

private fun snippetOf(body: String): String {
    val s = body.replace(Regex("\\s+"), " ").trim()
    return if (s.length <= 180) s else s.take(177).trimEnd() + "…"
}
