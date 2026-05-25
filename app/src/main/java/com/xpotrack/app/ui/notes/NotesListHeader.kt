package com.xpotrack.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xpotrack.app.R
import com.xpotrack.app.ui.components.DateTimeStrip
import com.xpotrack.app.ui.components.XpIconBtn
import com.xpotrack.app.ui.theme.XpTokens

@Composable
internal fun TopHalo() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                Brush.radialGradient(
                    0f to XpTokens.TealGlow,
                    0.7f to Color.Transparent,
                )
            )
    )
}

@Composable
internal fun NotesHeader(onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 18.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            DateTimeStrip()
            Spacer(Modifier.height(14.dp))
            Text("Notes", style = MaterialTheme.typography.displayLarge, color = XpTokens.Ink)
        }
        XpIconBtn(R.drawable.ic_search, "Search", tint = XpTokens.Ink2, border = true, onClick = onSearch)
    }
}

internal fun emptyCopy(filterId: Long?, categoryName: String?): Pair<String, String> = when {
    filterId == null -> "No notes yet" to "Tap + to write your first"
    filterId == 0L -> "Nothing uncategorized" to "Notes without a category land here"
    else -> "Nothing in ${categoryName ?: "this category"}" to "Tap + to add the first"
}
