package com.confused.onlylist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.confused.onlylist.data.mock.MockMedia
import com.confused.onlylist.designsystem.components.pressScale
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography

/**
 * MediaCard — grid card showing cover image + title + score.
 * Uses Coil AsyncImage for real cover images (falls back to color gradient).
 */
@Composable
fun MediaCard(
    media: MockMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    Column(
        modifier = modifier
            .pressScale(onClick = onClick)
            .clip(shapes.large),
    ) {
        // Cover image — uses Coil to load the real AniList cover URL.
        // Falls back to the media's cover color gradient if the URL is null/empty.
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            contentAlignment = Alignment.Center,
        ) {
            if (!media.coverImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = media.coverImageUrl,
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Color gradient fallback
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    media.coverColor,
                                    media.coverColor.copy(alpha = 0.6f),
                                )
                            )
                        ),
                )
            }

            // Score badge (top-right)
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(shapes.small)
                    .background(colors.background.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = media.score.toString(),
                    style = typography.caption.copy(color = colors.primary),
                )
            }
        }

        // Title + subtitle
        Column(Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)) {
            BasicText(
                text = media.title,
                style = typography.bodySmall.copy(color = colors.textPrimary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = "${media.format} · ${media.year}",
                style = typography.caption.copy(color = colors.textTertiary),
                maxLines = 1,
            )
        }
    }
}

/**
 * MediaListItem — horizontal row: cover + title + status + progress + score.
 */
@Composable
fun MediaListItem(
    media: MockMedia,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Cover thumbnail
        Box(
            Modifier
                .size(56.dp, 80.dp)
                .clip(shapes.medium),
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
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(media.coverColor, media.coverColor.copy(alpha = 0.6f))
                            )
                        ),
                )
            }
        }

        Column(
            Modifier.weight(1f),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        ) {
            BasicText(
                text = media.title,
                style = typography.titleMedium.copy(color = colors.textPrimary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(media.status, colors)
                BasicText(
                    text = "${media.progress}/${media.episodes} ep",
                    style = typography.caption.copy(color = colors.textSecondary),
                )
                if (media.nextAiringAt != null) {
                    BasicText(
                        text = "· ${media.nextAiringAt}",
                        style = typography.caption.copy(color = colors.primary),
                    )
                }
            }
        }

        BasicText(
            text = media.score.toString(),
            style = typography.numberMedium.copy(color = colors.textSecondary),
        )
    }
}

@Composable
private fun StatusDot(status: com.confused.onlylist.data.mock.MediaStatus, colors: com.confused.onlylist.designsystem.theme.OnlyListColors) {
    val color = when (status) {
        com.confused.onlylist.data.mock.MediaStatus.CURRENT -> colors.primary
        com.confused.onlylist.data.mock.MediaStatus.COMPLETED -> colors.success
        com.confused.onlylist.data.mock.MediaStatus.PAUSED -> colors.warning
        com.confused.onlylist.data.mock.MediaStatus.PLANNING -> colors.info
        com.confused.onlylist.data.mock.MediaStatus.DROPPED -> colors.error
        com.confused.onlylist.data.mock.MediaStatus.REPEATING -> Color(0xFFBB6BD9)
        com.confused.onlylist.data.mock.MediaStatus.AIRING -> colors.primary
    }
    Box(
        Modifier
            .size(8.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(color),
    )
}
