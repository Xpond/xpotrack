package com.xpotrack.app.ui.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

internal class SearchStaleness internal constructor(
    val clearing: Boolean,
    val arm: () -> Unit,
)

// Hide the LazyColumn between a search cancel and the new (unfiltered)
// page being ready. `arm()` is called from the CANCEL handler so this
// only triggers on cancel — typing keystrokes don't blank the list.
// isStale alone isn't enough because PagingData is emitted before SQLite
// returns rows; we watch loadState to call onMarkFresh only after a
// Loading → NotLoading cycle that started while isStale was true.
@Composable
internal fun rememberSearchStaleness(
    notes: LazyPagingItems<FeedItem.Note>,
    isStale: Boolean,
    searchOpen: Boolean,
    onMarkFresh: () -> Unit,
): SearchStaleness {
    var armed by remember { mutableStateOf(false) }
    var observedLoadingWhileStale by remember { mutableStateOf(false) }
    LaunchedEffect(isStale) {
        if (!isStale) {
            armed = false
            observedLoadingWhileStale = false
        }
    }
    LaunchedEffect(searchOpen) { if (searchOpen) armed = false }
    LaunchedEffect(isStale, notes.loadState.refresh) {
        if (!isStale) return@LaunchedEffect
        when (notes.loadState.refresh) {
            is LoadState.Loading -> observedLoadingWhileStale = true
            is LoadState.NotLoading -> if (observedLoadingWhileStale) onMarkFresh()
            else -> Unit
        }
    }
    return SearchStaleness(clearing = armed && isStale, arm = { armed = true })
}
