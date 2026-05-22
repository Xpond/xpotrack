package com.xpotrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xpotrack.app.ui.theme.XpTokens

@Composable
fun XpFab(iconRes: Int, contentDesc: String, modifier: Modifier = Modifier, shadow: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .size(56.dp)
            .then(if (shadow) Modifier.shadow(18.dp, CircleShape, ambientColor = XpTokens.Teal, spotColor = XpTokens.Teal) else Modifier)
            .clip(CircleShape).background(XpTokens.Teal).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDesc, tint = XpTokens.OnTeal, modifier = Modifier.size(22.dp))
    }
}
