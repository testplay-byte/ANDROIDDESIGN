package com.confused.onlylist.ui.screens.library

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.SegmentedControl
import com.confused.onlylist.designsystem.components.SkeletonListItem
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography

@Composable
fun LibraryScreen(
    onMediaClick: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current
    var selectedSegment by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 110.dp, bottom = 100.dp),
        ) {
            item {
                SegmentedControl(
                    options = listOf("Anime", "Manga", "All"),
                    selectedIndex = selectedSegment,
                    onSelected = { selectedSegment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(10) { index ->
                SkeletonListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        CollapsibleHeader(title = "Library", listState = listState)
    }
}
