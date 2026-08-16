package com.confused.onlylist.ui.screens.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.confused.onlylist.data.mock.MockData
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.GlassCard
import com.confused.onlylist.designsystem.components.pressScale
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * Modern Details screen — parallax banner, cover thumbnail, metadata chips,
 * genre chips, score ring, action buttons, expandable synopsis, episode list.
 * Per R-13 Topic 5 modern design patterns.
 */
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

    // Show loading state if media not loaded yet (no mock fallback)
    val displayMedia = media
    val isLoading = displayMedia == null

    Box(Modifier.fillMaxSize()) {
        if (isLoading) {
            // Loading state — no mock data
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Loading details...",
                    style = typography.bodyMedium.copy(color = colors.textTertiary),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().haze(hazeState),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // 1. Parallax banner with blur-on-collapse
                item {
                    BannerSection(media = displayMedia!!, listState = listState)
                }

                // 2. Title block — cover thumbnail + title + metadata
                item {
                    TitleBlock(media = displayMedia!!)
                }

                // 3. Genre chips (horizontal scroll)
                item {
                    if (displayMedia!!.genres.isNotEmpty()) {
                        GenreChips(genres = displayMedia!!.genres)
                    }
                }

                // 4. Score + action buttons
                item {
                    ScoreAndActions(media = displayMedia!!)
                }

                // 5. Synopsis (expandable)
                item {
                    SynopsisSection(synopsis = displayMedia!!.description)
                }

                // 6. Episodes header + list
                item {
                    EpisodesHeader(count = episodes.size)
                }
                items(episodes, key = { it.number }) { episode ->
                    EpisodeRow(episode = episode, coverColor = displayMedia!!.coverColor)
                }
            }

            // Back button (top-left, status bar padded)
            Box(
                Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(shapes.pill)
                    .background(colors.background.copy(alpha = 0.6f))
                    .pressScale(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "‹",
                    style = typography.headingLarge.copy(color = colors.textPrimary),
                )
            }

            CollapsibleHeader(title = displayMedia!!.title, listState = listState, hazeState = hazeState)
        }
    }
}

@Composable
private fun BannerSection(media: com.confused.onlylist.data.mock.MockMedia, listState: androidx.compose.foundation.lazy.LazyListState) {
    val colors = LocalColors.current
    val parallax = if (listState.firstVisibleItemIndex == 0) {
        listState.firstVisibleItemScrollOffset * 0.5f
    } else 0f
    val blurRadius = if (listState.firstVisibleItemIndex == 0) {
        (listState.firstVisibleItemScrollOffset / 200f).coerceIn(0f, 1f) * 16f
    } else 16f

    Box(
        Modifier
            .fillMaxWidth()
            .height(280.dp)
            .graphicsLayer { translationY = -parallax }
    ) {
        if (!media.coverImageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = media.coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius.dp),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(media.coverColor, colors.background)
                        )
                    ),
            )
        }
        // 3-stop gradient overlay
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            colors.background.copy(alpha = 0.7f),
                        )
                    )
                )
        )
    }
}

@Composable
private fun TitleBlock(media: com.confused.onlylist.data.mock.MockMedia) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-40).dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Cover thumbnail (100×150, overlaps banner)
        Box(
            Modifier
                .size(100.dp, 150.dp)
                .clip(shapes.large),
        ) {
            if (!media.coverImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = media.coverImageUrl,
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(media.coverColor),
                )
            }
        }

        // Title + metadata
        Column(
            Modifier.weight(1f).padding(top = 60.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicText(
                text = media.title,
                style = typography.titleLarge.copy(color = colors.textPrimary, fontWeight = FontWeight.Bold),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = "${media.format} · ${media.season} ${media.year}",
                style = typography.bodySmall.copy(color = colors.textSecondary),
            )
            BasicText(
                text = "${media.episodes} episodes",
                style = typography.caption.copy(color = colors.textTertiary),
            )
        }
    }
}

@Composable
private fun GenreChips(genres: List<String>) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    LazyRow(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(genres) { genre ->
            Box(
                Modifier
                    .clip(shapes.pill)
                    .background(colors.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                BasicText(
                    text = genre,
                    style = typography.caption.copy(color = colors.textSecondary),
                )
            }
        }
    }
}

@Composable
private fun ScoreAndActions(media: com.confused.onlylist.data.mock.MockMedia) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Score
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                text = media.score.toString(),
                style = typography.numberLarge.copy(color = colors.primary),
            )
            BasicText(
                text = "avg score",
                style = typography.caption.copy(color = colors.textTertiary),
            )
        }

        // Add to List button
        Box(
            Modifier
                .weight(1f)
                .height(44.dp)
                .clip(shapes.medium)
                .background(colors.primary)
                .pressScale { /* Phase 4: add to list */ },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "+ Add to List",
                style = typography.titleMedium.copy(color = colors.onPrimary),
            )
        }
    }
}

@Composable
private fun SynopsisSection(synopsis: String) {
    val colors = LocalColors.current
    val typography = LocalTypography.current
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        BasicText(
            text = if (expanded) synopsis else synopsis.take(150) + if (synopsis.length > 150) "..." else "",
            style = typography.bodyMedium.copy(color = colors.textSecondary),
        )
        if (synopsis.length > 150) {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .pressScale { expanded = !expanded },
            ) {
                BasicText(
                    text = if (expanded) "Show less" else "Read more",
                    style = typography.caption.copy(color = colors.primary),
                )
            }
        }
    }
}

@Composable
private fun EpisodesHeader(count: Int) {
    val typography = LocalTypography.current
    val colors = LocalColors.current
    BasicText(
        text = "Episodes ($count)",
        style = typography.titleMedium.copy(color = colors.textPrimary),
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun EpisodeRow(
    episode: EpisodeUi,
    coverColor: Color,
) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Thumbnail (160×90 or placeholder)
        Box(
            Modifier
                .size(120.dp, 68.dp)
                .clip(shapes.medium)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            coverColor.copy(alpha = 0.6f),
                            coverColor.copy(alpha = 0.3f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (!episode.thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = episode.thumbnailUrl,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                BasicText(
                    text = "EP ${episode.number}",
                    style = typography.caption.copy(color = colors.textTertiary),
                )
            }
        }

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicText(
                text = episode.title,
                style = typography.titleMedium.copy(color = colors.textPrimary),
            )
            if (episode.airDate.isNotEmpty()) {
                BasicText(
                    text = "Aired · ${episode.airDate}",
                    style = typography.caption.copy(color = colors.textTertiary),
                )
            }
            BasicText(
                text = episode.synopsis,
                style = typography.bodySmall.copy(color = colors.textSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
