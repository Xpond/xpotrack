package com.xpotrack.app.ui.categories

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.ui.theme.GeistMono
import com.xpotrack.app.ui.theme.XpTokens
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Hue ring + hex field. Saturation/value are fixed at a readable level so a
// tapped color always stays visible against XpTokens.Bg.
@Composable
fun HuePicker(hex: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HueRing(hex = hex, onChange = onChange, modifier = Modifier.size(72.dp))
        HexField(hex = hex, onChange = onChange, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HueRing(hex: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val hue = remember(hex) { hexToHue(hex) }
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { onChange(hueToHex(angleToHue(it, size.width.toFloat()))) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onChange(hueToHex(angleToHue(change.position, size.width.toFloat())))
                }
            },
    ) {
        val w = size.width
        val ringStroke = w * 0.18f
        val r = (w - ringStroke) / 2f
        drawCircle(
            brush = Brush.sweepGradient(HueStops),
            radius = r,
            style = Stroke(width = ringStroke),
        )
        if (hue != null) {
            val rad = (hue - 90f) * PI.toFloat() / 180f
            val cx = w / 2f + r * cos(rad)
            val cy = w / 2f + r * sin(rad)
            drawCircle(color = Color.White, radius = ringStroke * 0.55f, center = Offset(cx, cy))
            drawCircle(color = parseHexColor(hex), radius = ringStroke * 0.42f, center = Offset(cx, cy))
        }
    }
}

@Composable
private fun HexField(hex: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var raw by remember(hex) { mutableStateOf(hex.removePrefix("#").uppercase()) }
    Row(
        modifier.clip(RoundedCornerShape(8.dp)).background(XpTokens.Surface1)
            .border(0.5.dp, XpTokens.Hair, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(parseHexColor(hex))
            .border(0.5.dp, XpTokens.Hair, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(
            "#", fontFamily = GeistMono, fontWeight = FontWeight.Medium,
            fontSize = 13.sp, color = XpTokens.Ink3,
        )
        Spacer(Modifier.width(2.dp))
        BasicTextField(
            value = raw,
            onValueChange = { v ->
                val cleaned = v.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                raw = cleaned
                if (cleaned.length == 6) onChange("#$cleaned")
            },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = GeistMono, fontWeight = FontWeight.Medium,
                fontSize = 13.sp, color = XpTokens.Ink,
            ),
            cursorBrush = SolidColor(XpTokens.Teal),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (raw.isEmpty()) Text(
                        "RRGGBB", fontFamily = GeistMono, fontWeight = FontWeight.Medium,
                        fontSize = 13.sp, color = XpTokens.Ink3,
                    )
                    inner()
                }
            },
        )
    }
}

private val HueStops: List<Color> = (0..6).map {
    Color.hsv(hue = (it * 60f) % 360f, saturation = 0.75f, value = 0.95f)
}

private fun angleToHue(p: Offset, w: Float): Float {
    val dx = p.x - w / 2f
    val dy = p.y - w / 2f
    return (atan2(dy, dx) * 180f / PI.toFloat() + 90f + 360f) % 360f
}

private fun hueToHex(hue: Float): String {
    val c = Color.hsv(hue = hue, saturation = 0.75f, value = 0.95f)
    val r = (c.red * 255).toInt().coerceIn(0, 255)
    val g = (c.green * 255).toInt().coerceIn(0, 255)
    val b = (c.blue * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}

private fun hexToHue(hex: String): Float? = try {
    val c = parseHexColor(hex)
    val r = c.red; val g = c.green; val b = c.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val d = max - min
    if (d == 0f) null else {
        val h = when (max) {
            r -> 60f * (((g - b) / d) % 6f)
            g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }
        (h + 360f) % 360f
    }
} catch (_: Throwable) { null }
