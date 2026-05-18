package com.xpotrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens

enum class XpTab(val label: String, val iconRes: Int) {
    Notes("Notes",  R.drawable.ic_tab_notes),
    Tasks("Tasks",  R.drawable.ic_tab_tasks),
    Vault("Vault",  R.drawable.ic_tab_vault),
    // Enum value stays `More` to avoid churning every call site; the user-
    // facing label was renamed to "Settings" when milestone 13 replaced the
    // stub with the real screen.
    More("Settings", R.drawable.ic_tab_settings);
}

@Composable
fun XpBottomTabs(
    active: XpTab,
    onSelect: (XpTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.3f to XpTokens.Bg,
                    1f to XpTokens.Bg,
                )
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(XpTokens.Hair)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            XpTab.entries.forEach { t ->
                TabItem(tab = t, active = active == t, onClick = { onSelect(t) })
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun TabItem(tab: XpTab, active: Boolean, onClick: () -> Unit) {
    val color = if (active) XpTokens.Teal else XpTokens.Ink3
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = tab.label,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            tab.label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = color,
        )
    }
}
