package com.confused.onlylist.ui.screens.details

import androidx.compose.foundation.BasicText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import com.confused.onlylist.designsystem.components.SkeletonBox
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography

@Composable
fun DetailsScreen(
    mediaId: Int,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 110.dp, bottom = 32.dp),
        ) {
            // Cover banner placeholder
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(colors.surfaceVariant)
                )
            }
            // Title + metadata
            item {
                Column(Modifier.padding(16.dp)) {
                    BasicText(
                        text = "Media #$mediaId",
                        style = typography.displayMedium.copy(color = colors.textPrimary),
                    )
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        text = "— · — · —",
                        style = typography.bodyMedium.copy(color = colors.textSecondary),
                    )
                    Spacer(Modifier.height(12.dp))
                    BasicText(
                        text = "Description loading…",
                        style = typography.bodyMedium.copy(color = colors.textSecondary),
                    )
                }
            }
            // Episodes header
            item {
                BasicText(
                    text = "Episodes",
                    style = typography.titleMedium.copy(color = colors.textPrimary),
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                )
            }
            // Episode list placeholder (12 items)
            items(12) { ep ->
                androidx.compose.foundation.layout.Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                ) {
                    SkeletonBox(
                        modifier = Modifier
                            .height(64.dp)
                            .aspectRatio(16f / 9f)
                            .clip(shapes.medium),
                        cornerRadiusDp = 0.dp,
                    )
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    ) {
                        SkeletonBox(Modifier.fillMaxWidth(0.6f).height(14.dp))
                        SkeletonBox(Modifier.fillMaxWidth(0.9f).height(12.dp))
                        SkeletonBox(Modifier.fillMaxWidth(0.3f).height(12.dp))
                    }
                }
            }
        }
        CollapsibleHeader(title = "Details", listState = listState)
    }
}
