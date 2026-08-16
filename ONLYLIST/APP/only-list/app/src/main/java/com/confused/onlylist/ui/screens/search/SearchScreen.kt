package com.confused.onlylist.ui.screens.search

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.confused.onlylist.AppContainer
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.SegmentedControl
import com.confused.onlylist.designsystem.components.SkeletonBox
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography
import com.confused.onlylist.ui.components.MediaCard
import com.confused.onlylist.ui.components.toUiModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun SearchScreen(hazeState: HazeState, onMediaClick: (Int) -> Unit = {}) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current
    val viewModel: SearchViewModel = viewModel()
    var selectedSegment by remember { mutableIntStateOf(0) }

    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // When no query: show real trending from AniList (not mock data)
    val trending by AppContainer.mediaRepository.getTrending().collectAsState(initial = emptyList())
    val trendingMedia = trending.map { it.toUiModel() }

    val displayResults = when {
        query.isNotBlank() && results.isNotEmpty() -> results
        query.isBlank() && trendingMedia.isNotEmpty() -> trendingMedia
        else -> emptyList()
    }
    val isLoading = isRefreshing && displayResults.isEmpty()
    val statusText = when {
        query.isNotBlank() -> "${results.size} results (live from AniList)"
        trendingMedia.isNotEmpty() -> "${trendingMedia.size} trending (live from AniList)"
        isLoading -> "Loading from AniList..."
        else -> "No results"
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp),
        ) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(48.dp)
                        .clip(shapes.medium)
                        .background(colors.surfaceVariant)
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.primary),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        decorationBox = { innerTextField ->
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (query.isEmpty()) {
                                    BasicText(
                                        text = "Search anime or manga...",
                                        style = typography.bodyMedium.copy(color = colors.textTertiary),
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
            item {
                SegmentedControl(
                    options = listOf("Anime", "Manga"),
                    selectedIndex = selectedSegment,
                    onSelected = { selectedSegment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                BasicText(
                    text = statusText,
                    style = typography.caption.copy(color = colors.textTertiary),
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }

            if (isLoading) {
                // Skeleton loading
                val rowCount = 3
                for (rowIndex in 0 until rowCount) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            for (colIndex in 0..1) {
                                Box(Modifier.weight(1f)) {
                                    SkeletonBox(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(2f / 3f),
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (displayResults.isNotEmpty()) {
                val rowCount = (displayResults.size + 1) / 2
                for (rowIndex in 0 until rowCount) {
                    item(key = "row-$rowIndex") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            for (colIndex in 0..1) {
                                val index = rowIndex * 2 + colIndex
                                if (index < displayResults.size) {
                                    Box(Modifier.weight(1f)) {
                                        MediaCard(
                                            media = displayResults[index],
                                            onClick = { onMediaClick(displayResults[index].id) },
                                        )
                                    }
                                } else {
                                    Box(Modifier.weight(1f))
                                }
                            }
                        }
                    }
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
                            text = "No results. Try searching for an anime title.",
                            style = typography.bodyMedium.copy(color = colors.textTertiary),
                        )
                    }
                }
            }
        }
        CollapsibleHeader(title = "Search", listState = listState, hazeState = hazeState)
    }
}
