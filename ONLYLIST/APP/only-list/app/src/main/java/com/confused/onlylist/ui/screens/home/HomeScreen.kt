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
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun HomeScreen() {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current

    // Offline-first: observe Room data + trigger network refresh
    val trending by AppContainer.mediaRepository.getTrending().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        Logger.d("Home", "Refreshing trending from AniList...")
        val result = AppContainer.mediaRepository.refreshTrending()
        result.fold(
            onSuccess = { Logger.d("Home", "Trending refresh OK") },
            onFailure = { Logger.w("Home", "Trending refresh failed: ${it.message}") },
        )
    }

    // Use real data if available, otherwise mock data
    val trendingMedia = if (trending.isNotEmpty()) {
        trending.map { it.toUiModel() }
    } else {
        MockData.trending
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 110.dp, bottom = 100.dp),
        ) {
            // Welcome banner
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    BasicText(
                        text = "Welcome to Only-List",
                        style = typography.headingLarge.copy(color = colors.textPrimary),
                    )
                    BasicText(
                        text = "Your anime & manga tracker",
                        style = typography.bodyMedium.copy(color = colors.textSecondary),
                    )
                }
            }

            // Quick stats
            item {
                SectionHeader("Quick Stats")
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

            // Trending now (grid — 2 per row)
            item {
                SectionHeader("Trending Now")
            }
            val rowCount = (trendingMedia.size + 1) / 2
            for (rowIndex in 0 until rowCount) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        for (colIndex in 0..1) {
                            val index = rowIndex * 2 + colIndex
                            if (index < trendingMedia.size) {
                                Box(Modifier.weight(1f)) {
                                    MediaCard(
                                        media = trendingMedia[index],
                                        onClick = { /* Phase 2: navigate to details */ },
                                    )
                                }
                            } else {
                                Box(Modifier.weight(1f))
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
                    onClick = { /* Phase 2: navigate to details */ },
                )
            }
        }
        CollapsibleHeader(title = "Home", listState = listState)
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
