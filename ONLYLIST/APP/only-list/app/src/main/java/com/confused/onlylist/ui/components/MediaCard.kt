package com.confused.onlylist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.confused.onlylist.data.mock.MediaStatus
import com.confused.onlylist.data.mock.MockMedia
import com.confused.onlylist.designsystem.components.pressScale
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography
import com.confused.onlylist.designsystem.theme.OnlyListColors

/**
 * A card showing an anime/manga entry — cover art placeholder + title + subtitle.
 * Per DESIGN-LANGUAGE.md §7.7 (adapted for grid layout).
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
        // Cover art placeholder — uses the media's cover color as a gradient
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            media.coverColor,
                            media.coverColor.copy(alpha = 0.6f),
                        )
                    )
                ),
            contentAlignment = Alignment.BottomStart,
        ) {
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
 * A horizontal list-item row — cover + title + status + progress + score.
 * For list views (Library, Search results).
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Cover thumbnail
        Box(
            Modifier
                .size(56.dp, 80.dp)
                .clip(shapes.medium)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(media.coverColor, media.coverColor.copy(alpha = 0.6f))
                    )
                ),
        )

        // Info
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicText(
                text = media.title,
                style = typography.titleMedium.copy(color = colors.textPrimary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        // Score
        BasicText(
            text = media.score.toString(),
            style = typography.numberMedium.copy(color = colors.textSecondary),
        )
    }
}

@Composable
private fun StatusDot(status: MediaStatus, colors: OnlyListColors) {
    val color = when (status) {
        MediaStatus.CURRENT -> colors.primary
        MediaStatus.COMPLETED -> colors.success
        MediaStatus.PAUSED -> colors.warning
        MediaStatus.PLANNING -> colors.info
        MediaStatus.DROPPED -> colors.error
        MediaStatus.REPEATING -> Color(0xFFBB6BD9)
        MediaStatus.AIRING -> colors.primary
    }
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color),
    )
}
