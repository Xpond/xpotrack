package com.xpotrack.app.ui.alarm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.GeistMono
import com.xpotrack.app.ui.theme.XpTokens
import kotlin.math.roundToInt

@Composable
fun AlarmRingingScreen(
    title: String,
    time: String,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0f to Color(0xFF0C2A26),
                    0.5f to XpTokens.Bg,
                    1f to XpTokens.Bg,
                )
            )
            .systemBarsPadding()
            .padding(horizontal = 26.dp)
            .padding(top = 12.dp, bottom = 18.dp),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                PulseRings()
                TimeAndLabel(title = title, time = time)
            }
            SnoozeRow()
            Spacer(Modifier.height(14.dp))
            SlideToDismiss(onDismiss = onDismiss)
        }
    }
}

@Composable
private fun PulseRings() {
    var t by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) t = withFrameNanos { it } - start
    }
    val cycleNs = 2_400_000_000L
    val a = ((t % cycleNs).toFloat() / cycleNs).coerceIn(0f, 1f)
    val b = (((t + cycleNs / 3) % cycleNs).toFloat() / cycleNs).coerceIn(0f, 1f)
    Box(Modifier.size(320.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0f to XpTokens.Teal.copy(alpha = 0.10f),
                        0.7f to Color.Transparent,
                    )
                )
        )
        Canvas(Modifier.fillMaxSize()) {
            drawPulseRing(progress = b, baseRadiusDp = 140f)
            drawPulseRing(progress = a, baseRadiusDp = 110f)
        }
    }
}

// Matches alarm.jsx @keyframes xp-pulse:
//   0%   scale 0.92, opacity 0.8
//   60%  scale 1.18, opacity 0
//   100% scale 1.22, opacity 0
private fun DrawScope.drawPulseRing(progress: Float, baseRadiusDp: Float) {
    val scale = when {
        progress < 0.6f -> 0.92f + (1.18f - 0.92f) * (progress / 0.6f)
        else -> 1.18f + (1.22f - 1.18f) * ((progress - 0.6f) / 0.4f)
    }
    val ringAlpha = when {
        progress < 0.6f -> 0.85f * (1f - progress / 0.6f)
        else -> 0f
    }
    val radius = baseRadiusDp.dp.toPx() * scale
    drawCircle(
        color = Color(0xFF5EEAD4).copy(alpha = ringAlpha),
        radius = radius,
        center = Offset(size.width / 2f, size.height / 2f),
        style = Stroke(width = 2.dp.toPx()),
    )
}

@Composable
private fun TimeAndLabel(title: String, time: String) {
    val (hh, mm, suffix) = remember(time) { splitClock(time) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "$hh:$mm",
                color = XpTokens.Ink,
                style = TextStyle(
                    fontFamily = GeistMono,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.02).em,
                    lineHeight = 64.sp,
                ),
            )
            Spacer(Modifier.size(6.dp))
            Text(
                suffix,
                color = XpTokens.Ink3,
                style = TextStyle(
                    fontFamily = GeistMono,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = XpTokens.Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SnoozeRow() {
    Column {
        Text(
            "SNOOZE",
            color = XpTokens.Ink3,
            style = TextStyle(
                fontFamily = GeistMono, fontSize = 10.sp,
                fontWeight = FontWeight.Medium, letterSpacing = 0.06.em,
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("5m", "15m", "1h").forEach { s ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(XpTokens.Teal.copy(alpha = 0.06f))
                        .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        s, color = XpTokens.Teal,
                        style = TextStyle(
                            fontFamily = GeistMono, fontSize = 14.sp,
                            fontWeight = FontWeight.Medium, letterSpacing = 0.02.em,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideToDismiss(onDismiss: () -> Unit) {
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableStateOf(0f) }
    var dragPx by remember { mutableStateOf(0f) }
    val knobPx = with(density) { 64.dp.toPx() }
    Box(
        Modifier
            .fillMaxWidth(0.78f)
            .height(72.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(999.dp))
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    trackWidthPx = size.width.toFloat()
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragPx >= trackWidthPx - knobPx - 4f) onDismiss()
                            else dragPx = 0f
                        },
                        onHorizontalDrag = { _, delta ->
                            dragPx = (dragPx + delta).coerceIn(0f, (trackWidthPx - knobPx).coerceAtLeast(0f))
                        },
                    )
                },
        ) {
            Text(
                "Slide to dismiss",
                color = XpTokens.Ink2,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Center),
            )
            Box(
                Modifier
                    .offset { IntOffset(dragPx.roundToInt(), 0) }
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(XpTokens.Teal)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center,
            ) {
                Text("›", color = XpTokens.OnTeal, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun splitClock(time: String): Triple<String, String, String> {
    if (!time.contains(":")) return Triple("0", "00", "AM")
    val (hStr, mStr) = time.split(":")
    val h = hStr.toIntOrNull() ?: 0
    val m = mStr.toIntOrNull() ?: 0
    val isPm = h >= 12
    val hh = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return Triple(hh.toString(), "%02d".format(m), if (isPm) "PM" else "AM")
}
