package com.confused.onlylist.ui.screens.details

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.confused.onlylist.data.mock.MockData
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun DetailsScreen(
    mediaId: Int,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val hazeState = remember { HazeState() }
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current
    val viewModel: DetailsViewModel = viewModel(key = "details-$mediaId") {
        DetailsViewModel(mediaId)
    }

    val media by viewModel.media.collectAsState()
    val episodes by viewModel.episodes.collectAsState()

    // Use real data if available, otherwise mock fallback
    val displayMedia = media ?: MockData.trending.find { it.id == mediaId } ?: MockData.trending.first()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 130.dp, bottom = 32.dp),
        ) {
            // Cover banner — real image via Coil
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                ) {
                    if (!displayMedia.coverImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = displayMedia.coverImageUrl,
                            contentDescription = displayMedia.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(displayMedia.coverColor, colors.background)
                                    )
                                ),
                        )
                    }
                    // Gradient overlay for readability
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        colors.background.copy(alpha = 0.7f),
                                    )
                                )
                            )
                    )
                }
            }

            // Title + metadata
            item {
                Column(Modifier.padding(16.dp)) {
                    BasicText(
                        text = displayMedia.title,
                        style = typography.displayMedium.copy(color = colors.textPrimary),
                    )
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        text = "${displayMedia.format} · ${displayMedia.season} ${displayMedia.year} · ${displayMedia.episodes} eps",
                        style = typography.bodyMedium.copy(color = colors.textSecondary),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        displayMedia.genres.take(3).forEach { genre ->
                            Box(
                                Modifier
                                    .clip(shapes.small)
                                    .background(colors.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                BasicText(
                                    text = genre,
                                    style = typography.caption.copy(color = colors.textSecondary),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    BasicText(
                        text = displayMedia.description,
                        style = typography.bodyMedium.copy(color = colors.textSecondary),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BasicText(
                            text = displayMedia.score.toString(),
                            style = typography.numberLarge.copy(color = colors.primary),
                        )
                        BasicText(
                            text = "avg score",
                            style = typography.caption.copy(color = colors.textTertiary),
                        )
                    }
                }
            }

            // Episodes header
            item {
                BasicText(
                    text = "Episodes (${episodes.size})",
                    style = typography.titleMedium.copy(color = colors.textPrimary),
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                )
            }

            // Episode list
            items(episodes) { episode ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Thumbnail placeholder
                    Box(
                        Modifier
                            .size(120.dp, 68.dp)
                            .clip(shapes.medium)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        displayMedia.coverColor.copy(alpha = 0.6f),
                                        displayMedia.coverColor.copy(alpha = 0.3f),
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "EP ${episode.number}",
                            style = typography.caption.copy(color = colors.textTertiary),
                        )
                    }

                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        BasicText(
                            text = episode.title,
                            style = typography.titleMedium.copy(color = colors.textPrimary),
                        )
                        BasicText(
                            text = "Aired · ${episode.airDate}",
                            style = typography.caption.copy(color = colors.textTertiary),
                        )
                        BasicText(
                            text = episode.synopsis,
                            style = typography.bodySmall.copy(color = colors.textSecondary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        CollapsibleHeader(title = "Details", listState = listState, hazeState = hazeState)
    }
}
