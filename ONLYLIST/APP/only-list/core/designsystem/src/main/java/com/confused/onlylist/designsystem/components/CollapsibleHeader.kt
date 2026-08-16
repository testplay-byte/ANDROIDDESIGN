package com.confused.onlylist.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

// ── Collapsible header with progressive gradient blur ──
//
// FIX (user feedback): at scroll=0, the header was a completely dark box
// because backgroundColor = colors.background was always opaque.
//
// NOW: backgroundColor alpha transitions 0 → 1 with scroll.
// - At scroll=0: header is TRANSPARENT (content shows through, no dark box).
// - On scroll: bg becomes opaque + frosted glass fades in.
// The text-blur issue only matters when scrolled (when scrimAlpha > 0),
// by which point backgroundColor is opaque enough for Haze to work.

@Composable
fun CollapsibleHeader(
    title: String,
    listState: LazyListState,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColors.current
    val typography = LocalTypography.current

    val rawOffset = if (listState.firstVisibleItemIndex == 0) {
        listState.firstVisibleItemScrollOffset.toFloat()
    } else {
        Float.MAX_VALUE
    }
    val collapseFraction = (rawOffset / 200f).coerceIn(0f, 1f)

    val animatedFraction by animateFloatAsState(
        targetValue = collapseFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "headerCollapse",
    )

    val titleFontSize = lerp(45.sp, 24.sp, animatedFraction)
    val titleStyle = typography.displayLarge.copy(
        fontSize = titleFontSize,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
    )

    val topPad = lerp(8.dp, 2.dp, animatedFraction)
    val bottomPad = lerp(4.dp, 0.dp, animatedFraction)

    // Scrim alpha: 0 → 0.7
    val scrimAlpha = animatedFraction * 0.7f

    // FIX: backgroundColor alpha transitions 0 → 1 with scroll.
    // At scroll=0: transparent (no dark box). On scroll: opaque (blur works on text).
    val bgAlpha = animatedFraction

    Box(
        modifier
            .fillMaxWidth()
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = colors.background.copy(alpha = bgAlpha),
                    blurRadius = 28.dp,
                    tints = listOf(HazeTint(colors.surface.copy(alpha = scrimAlpha))),
                ),
            ) {
                // Progressive gradient blur — full frost at top → no blur at bottom edge.
                // Only visible when scrolled (bgAlpha > 0).
                progressive = HazeProgressive.verticalGradient(
                    startIntensity = 1f,
                    endIntensity = 0f,
                )
            }
            .statusBarsPadding()
    ) {
        BasicText(
            text = title,
            style = titleStyle,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = topPad, bottom = bottomPad),
        )
    }
}
