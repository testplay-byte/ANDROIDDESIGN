package com.confused.onlylist.ui.screens.airing

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography

@Composable
fun AiringScreen() {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 110.dp, bottom = 100.dp),
        ) {
            item {
                // "Next airing" card placeholder
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(shapes.large)
                        .background(colors.surfaceVariant)
                        .padding(16.dp)
                ) {
                    BasicText(
                        text = "Next airing",
                        style = typography.caption.copy(color = colors.textTertiary),
                    )
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        text = "—",
                        style = typography.titleLarge.copy(color = colors.textPrimary),
                    )
                    BasicText(
                        text = "Episode — in —",
                        style = typography.bodyMedium.copy(color = colors.textSecondary),
                    )
                }
            }
            // Day-by-day schedule placeholder
            val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            days.forEach { day ->
                item {
                    BasicText(
                        text = day,
                        style = typography.titleMedium.copy(color = colors.textSecondary),
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    )
                    com.confused.onlylist.designsystem.components.SkeletonListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
        }
        CollapsibleHeader(title = "Airing", listState = listState)
    }
}
