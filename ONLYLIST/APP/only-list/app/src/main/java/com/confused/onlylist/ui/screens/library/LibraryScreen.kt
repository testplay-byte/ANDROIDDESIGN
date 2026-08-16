package com.confused.onlylist.ui.screens.library

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.SegmentedControl
import com.confused.onlylist.designsystem.components.SkeletonBox
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography
import com.confused.onlylist.ui.components.MediaListItem
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun LibraryScreen(
    hazeState: HazeState,
    onMediaClick: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current
    val viewModel: LibraryViewModel = viewModel()
    var selectedSegment by remember { mutableIntStateOf(0) }

    val library by viewModel.library.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val isLoading = isRefreshing && library.isEmpty()

    val filteredMedia = when (selectedSegment) {
        0 -> library.filter { it.status.name in listOf("CURRENT", "AIRING") }
        1 -> library.filter { it.status.name == "COMPLETED" }
        2 -> library
        else -> library
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp),
        ) {
            item {
                SegmentedControl(
                    options = listOf("Watching", "Completed", "All"),
                    selectedIndex = selectedSegment,
                    onSelected = { selectedSegment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                val statusText = when {
                    isLoading -> "Loading from AniList..."
                    filteredMedia.isNotEmpty() -> "${filteredMedia.size} entries (live from AniList)"
                    else -> "No entries found"
                }
                BasicText(
                    text = statusText,
                    style = typography.caption.copy(color = colors.textTertiary),
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }

            if (isLoading) {
                items(8) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            } else if (filteredMedia.isNotEmpty()) {
                items(filteredMedia, key = { it.id }) { media ->
                    MediaListItem(
                        media = media,
                        onClick = { onMediaClick(media.id) },
                    )
                }
            } else {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "No entries. Pull to refresh or check your connection.",
                            style = typography.bodyMedium.copy(color = colors.textTertiary),
                        )
                    }
                }
            }
        }
        CollapsibleHeader(title = "Library", listState = listState, hazeState = hazeState)
    }
}
