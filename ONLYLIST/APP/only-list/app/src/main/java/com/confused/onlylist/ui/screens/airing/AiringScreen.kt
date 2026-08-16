package com.confused.onlylist.ui.screens.airing

import androidx.compose.foundation.BasicText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.confused.onlylist.data.mock.MockData
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.GlassCard
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography
import com.confused.onlylist.ui.components.MediaListItem

@Composable
fun AiringScreen() {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 110.dp, bottom = 100.dp),
        ) {
            // Next airing highlight card
            item {
                val next = MockData.airingToday.firstOrNull()
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
                            text = "Episode ${next.nextEpisode} · ${next.nextAiringAt}",
                            style = typography.bodyMedium.copy(color = colors.primary),
                        )
                    }
                }
            }

            // Weekly schedule
            item {
                SectionHeader("This Week")
            }
            items(MockData.airingThisWeek.size) { index ->
                val media = MockData.airingThisWeek[index]
                MediaListItem(
                    media = media,
                    onClick = { /* Phase 2: navigate to details */ },
                )
            }
        }
        CollapsibleHeader(title = "Airing", listState = listState)
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
