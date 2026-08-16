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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.confused.onlylist.data.mock.MockData
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.GlassCard
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography
import com.confused.onlylist.ui.components.MediaListItem
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun AiringScreen(hazeState: HazeState) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current
    val viewModel: AiringViewModel = viewModel()

    val airing by viewModel.airing.collectAsState()

    // Use real AniList airing data if available, otherwise mock data
    val displayAiring = if (airing.isNotEmpty()) airing else MockData.airingThisWeek

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp),
        ) {
            // Next airing highlight card
            item {
                val next = displayAiring.firstOrNull { it.nextAiringAt != null } ?: displayAiring.firstOrNull()
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
                }
            }

            item {
                SectionHeader("Airing This Week" + if (airing.isNotEmpty()) " (live)" else "")
            }
            items(displayAiring) { media ->
                MediaListItem(
                    media = media,
                    onClick = { /* Phase 3: navigate to details */ },
                )
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
