package com.xpotrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xpotrack.app.R
import com.xpotrack.app.ui.theme.XpTokens

// Floating bottom action bar shown while the user is multi-selecting notes.
// The screen positions it just above the tab bar; FAB is hidden in the same state.
@Composable
fun SelectionBar(
    count: Int,
    allSelected: Boolean,
    onToggleAll: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(XpTokens.Surface1.copy(alpha = 0.82f))
            .border(0.5.dp, XpTokens.Hair2, RoundedCornerShape(22.dp))
            .padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$count selected",
            style = MaterialTheme.typography.titleSmall,
            color = XpTokens.Ink,
        )
        Spacer(Modifier.width(14.dp))
        CheckBoxToggle(allSelected, onToggleAll)
        Spacer(Modifier.width(10.dp))
        XpIconBtn(R.drawable.ic_share, "Share", tint = XpTokens.Ink, onClick = onShare)
        Spacer(Modifier.width(10.dp))
        XpIconBtn(R.drawable.ic_trash, "Delete", tint = XpTokens.Danger, onClick = onDelete)
    }
}

@Composable
private fun CheckBoxToggle(checked: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (checked) XpTokens.Teal else XpTokens.Surface1)
                .border(
                    1.dp,
                    if (checked) XpTokens.Teal else XpTokens.Ink3,
                    RoundedCornerShape(5.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    "All selected",
                    tint = XpTokens.OnTeal,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
