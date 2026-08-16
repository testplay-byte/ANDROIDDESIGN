package com.confused.onlylist.ui.screens.home

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.confused.onlylist.AppContainer
import com.confused.onlylist.common.Logger
import com.confused.onlylist.data.mock.MockData
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.GlassCard
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography
import com.confused.onlylist.ui.components.MediaCard
import com.confused.onlylist.ui.components.MediaListItem
import com.confused.onlylist.ui.components.toUiModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun HomeScreen(hazeState: HazeState, onMediaClick: (Int) -> Unit = {}) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current

    val trending by AppContainer.mediaRepository.getTrending().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        Logger.d("Home", "Refreshing trending from AniList...")
        val result = AppContainer.mediaRepository.refreshTrending()
        result.fold(
            onSuccess = { Logger.d("Home", "Trending refresh OK — ${trending.size} cached") },
            onFailure = { Logger.w("Home", "Trending refresh failed: ${it.message}") },
        )
    }

    val trendingMedia = if (trending.isNotEmpty()) {
        trending.map { it.toUiModel() }
    } else {
        MockData.trending
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState),
            contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp),
        ) {
            // Trending Now — horizontal scroll carousel
            item {
                SectionHeader("Trending Now")
            }
            item {
                LazyRow(
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(trendingMedia.take(10)) { media ->
                        Box(Modifier.width(140.dp)) {
                            MediaCard(
                                media = media,
                                onClick = { onMediaClick(media.id) },
                            )
                        }
                    }
                }
            }

            // Quick stats
            item {
                SectionHeader("Your Stats")
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    val stats = listOf(
                        "Trending" to trendingMedia.size.toString(),
                        "Watching" to MockData.currentlyWatching.size.toString(),
                        "Completed" to MockData.completed.size.toString(),
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        stats.forEach { (label, value) ->
                            Column {
                                BasicText(
                                    text = value,
                                    style = typography.numberLarge.copy(color = colors.primary),
                                )
                                BasicText(
                                    text = label,
                                    style = typography.caption.copy(color = colors.textTertiary),
                                )
                            }
                        }
                    }
                }
            }

            // Currently Watching (list)
            item {
                SectionHeader("Currently Watching")
            }
            items(MockData.currentlyWatching) { media ->
                MediaListItem(
                    media = media,
                    onClick = { onMediaClick(media.id) },
                )
            }
        }
        CollapsibleHeader(title = "Home", listState = listState, hazeState = hazeState)
    }
}

@Composable
private fun SectionHeader(title: String) {
    val typography = LocalTypography.current
    val colors = LocalColors.current
    BasicText(
        text = title,
        style = typography.titleMedium.copy(color = colors.textSecondary),
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}
