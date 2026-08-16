package com.confused.onlylist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.confused.onlylist.data.mock.MediaStatus
import com.confused.onlylist.data.mock.MockMedia
import com.confused.onlylist.designsystem.components.pressScale
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography
import com.confused.onlylist.designsystem.theme.OnlyListColors

/**
 * MediaCard — modern overlay design (R-13).
 * Title + score overlaid ON the cover via a bottom gradient scrim.
 * Score badge top-right (black pill + ★). Status badge top-left.
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

    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(shapes.large)
            .pressScale(pressedScale = 0.96f, onClick = onClick),
    ) {
        // 1. Cover image — full bleed (or gradient fallback)
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
                        Brush.verticalGradient(
                            colors = listOf(
                                media.coverColor,
                                media.coverColor.copy(alpha = 0.4f),
                            )
                        )
                    ),
            )
        }

        // 2. Bottom gradient scrim for title readability (transparent → black 70%)
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                        )
                    )
                )
        )

        // 3. Title overlaid on cover (bottom)
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            BasicText(
                text = media.title,
                style = typography.bodySmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = "${media.format} · ${media.year}",
                style = typography.caption.copy(color = Color.White.copy(alpha = 0.7f)),
                maxLines = 1,
            )
        }

        // 4. Score badge (top-right) — black pill + ★
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .clip(shapes.small)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "★",
                style = typography.caption.copy(color = colors.warning),
            )
            BasicText(
                text = media.score.toString(),
                style = typography.caption.copy(color = Color.White),
            )
        }
    }
}

/**
 * MediaListItem — modern list row with cover + info + status.
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
            .pressScale(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Cover thumbnail (rounded)
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
                            Brush.verticalGradient(
                                colors = listOf(media.coverColor, media.coverColor.copy(alpha = 0.6f))
                            )
                        ),
                )
            }
        }

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
