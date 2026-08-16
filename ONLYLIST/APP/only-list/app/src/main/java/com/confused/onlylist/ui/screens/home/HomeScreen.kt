package com.confused.onlylist.ui.screens.home

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.confused.onlylist.AppContainer
import com.confused.onlylist.common.Logger
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.GlassCard
import com.confused.onlylist.designsystem.components.SkeletonBox
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography
import com.confused.onlylist.ui.components.MediaCard
import com.confused.onlylist.ui.components.toUiModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun HomeScreen(hazeState: HazeState, onMediaClick: (Int) -> Unit = {}) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current

    val trending by AppContainer.mediaRepository.getTrending().collectAsState(initial = emptyList())
    var isRefreshing by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        Logger.d("Home", "Refreshing trending from AniList...")
        val result = AppContainer.mediaRepository.refreshTrending()
        result.fold(
            onSuccess = { Logger.d("Home", "Trending refresh OK — ${trending.size} cached") },
            onFailure = { Logger.w("Home", "Trending refresh failed: ${it.message}") },
        )
        isRefreshing = false
    }

    val trendingMedia = trending.map { it.toUiModel() }
    val isLoading = isRefreshing && trendingMedia.isEmpty()

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
                if (isLoading) {
                    // Skeleton loading state
                    LazyRow(
                        Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(5) {
                            Box(Modifier.width(140.dp)) {
                                SkeletonBox(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(2f / 3f),
                                )
                            }
                        }
                    }
                } else if (trendingMedia.isNotEmpty()) {
                    LazyRow(
                        Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(trendingMedia.take(10), key = { it.id }) { media ->
                            Box(Modifier.width(140.dp)) {
                                MediaCard(
                                    media = media,
                                    onClick = { onMediaClick(media.id) },
                                )
                            }
                        }
                    }
                } else {
                    // Empty/error state — no mock data
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        BasicText(
                            text = "Failed to load trending. Check your connection.",
                            style = typography.bodyMedium.copy(color = colors.textTertiary),
                        )
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
                        "Status" to if (isLoading) "Loading..." else "Ready",
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
