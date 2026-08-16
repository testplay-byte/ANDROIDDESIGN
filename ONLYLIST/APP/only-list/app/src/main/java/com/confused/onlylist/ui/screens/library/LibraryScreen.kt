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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.confused.onlylist.data.mock.MediaStatus
import com.confused.onlylist.data.mock.MockData
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.SegmentedControl
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography
import com.confused.onlylist.ui.components.MediaListItem

@Composable
fun LibraryScreen(
    onMediaClick: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current
    var selectedSegment by remember { mutableIntStateOf(0) }

    // Filter by status for each segment
    val allMedia = (MockData.currentlyWatching + MockData.completed + MockData.trending).distinctBy { it.id }
    val filteredMedia = when (selectedSegment) {
        0 -> allMedia.filter { it.status == MediaStatus.CURRENT }
        1 -> allMedia.filter { it.status == MediaStatus.COMPLETED }
        2 -> allMedia
        else -> allMedia
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 110.dp, bottom = 100.dp),
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
                BasicText(
                    text = "${filteredMedia.size} entries",
                    style = typography.caption.copy(color = colors.textTertiary),
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }
            items(filteredMedia) { media ->
                MediaListItem(
                    media = media,
                    onClick = { onMediaClick(media.id) },
                )
            }
        }
        CollapsibleHeader(title = "Library", listState = listState)
    }
}
