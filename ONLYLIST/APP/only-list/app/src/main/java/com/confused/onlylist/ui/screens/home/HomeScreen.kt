package com.confused.onlylist.ui.screens.home

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.confused.onlylist.data.mock.MockData
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.GlassCard
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography
import com.confused.onlylist.ui.components.MediaCard
import com.confused.onlylist.ui.components.MediaListItem

@Composable
fun HomeScreen() {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current

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
                        "Watching" to MockData.currentlyWatching.size.toString(),
                        "Completed" to MockData.completed.size.toString(),
                        "Airing" to MockData.airingToday.size.toString(),
                    )
                    androidx.compose.foundation.layout.Row(
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

            // Trending now (grid)
            item {
                SectionHeader("Trending Now")
            }
            // Grid items — we use a nested LazyVerticalGrid inside the LazyColumn item.
            // This is fine for small datasets; Phase 2 will use a real grid with nested scroll.
            item {
                androidx.compose.foundation.layout.Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MockData.trending.take(2).forEach { media ->
                        Box(Modifier.weight(1f)) {
                            MediaCard(
                                media = media,
                                onClick = { /* Phase 2: navigate to details */ },
                            )
                        }
                    }
                }
            }
            item {
                androidx.compose.foundation.layout.Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MockData.trending.drop(2).take(2).forEach { media ->
                        Box(Modifier.weight(1f)) {
                            MediaCard(
                                media = media,
                                onClick = { /* Phase 2: navigate to details */ },
                            )
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
