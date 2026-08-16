package com.confused.onlylist.ui.screens.airing

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.GlassCard
import com.confused.onlylist.designsystem.components.SkeletonBox
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography
import com.confused.onlylist.ui.components.MediaListItem
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun AiringScreen(hazeState: HazeState, onMediaClick: (Int) -> Unit = {}) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current
    val viewModel: AiringViewModel = viewModel()

    val airing by viewModel.airing.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val isLoading = isRefreshing && airing.isEmpty()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp),
        ) {
            // Next airing highlight card
            item {
                val next = airing.firstOrNull { it.nextAiringAt != null } ?: airing.firstOrNull()
                if (next != null) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        BasicText(
                            text = "NEXT AIRING",
                            style = typography.caption.copy(color = colors.textTertiary),
                        )
                        Spacer(Modifier.height(4.dp))
                        BasicText(
                            text = next.title,
                            style = typography.titleLarge.copy(color = colors.textPrimary),
                        )
                        BasicText(
                            text = if (next.nextEpisode != null) "Episode ${next.nextEpisode} · ${next.nextAiringAt ?: "soon"}" else "Airing soon",
                            style = typography.bodyMedium.copy(color = colors.primary),
                        )
                    }
                } else if (isLoading) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        SkeletonBox(
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                        )
                    }
                }
            }

            item {
                val headerText = when {
                    isLoading -> "Loading from AniList..."
                    airing.isNotEmpty() -> "Airing This Week (live)"
                    else -> "Airing This Week"
                }
                SectionHeader(headerText)
            }

            if (isLoading) {
                items(5) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            } else if (airing.isNotEmpty()) {
                items(airing, key = { it.id }) { media ->
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
                            text = "No airing anime found. Pull to refresh.",
                            style = typography.bodyMedium.copy(color = colors.textTertiary),
                        )
                    }
                }
            }
        }
        CollapsibleHeader(title = "Airing", listState = listState, hazeState = hazeState)
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
