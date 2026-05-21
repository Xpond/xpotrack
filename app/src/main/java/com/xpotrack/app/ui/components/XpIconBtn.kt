package com.xpotrack.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun XpIconBtn(
    iconRes: Int,
    contentDesc: String,
    tint: Color = XpTokens.Ink2,
    size: Dp = 38.dp,
    iconSize: Dp = 18.dp,
    border: Boolean = false,
    onClick: () -> Unit = {},
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .then(if (border) Modifier.border(0.5.dp, XpTokens.Hair2, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDesc, tint = tint, modifier = Modifier.size(iconSize))
    }
}
