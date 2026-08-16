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

// ── Collapsible header with PROGRESSIVE gradient blur (R-13 fix) ──
//
// R-13 FIX: added HazeProgressive.verticalGradient for a gradient blur edge
// at the bottom of the header (full frost at top → no blur at bottom).
// This is a REAL progressive blur (not a color scrim) — the iOS-style
// backdrop-filter gradient.

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

    // Scrim alpha: 0 → 0.7 (reduced from 0.85 — was too dark).
    val scrimAlpha = animatedFraction * 0.7f

    Box(
        modifier
            .fillMaxWidth()
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = colors.background,
                    blurRadius = 28.dp,
                    tints = listOf(HazeTint(colors.surface.copy(alpha = scrimAlpha))),
                ),
            ) {
                // R-13: PROGRESSIVE gradient blur — full frost at top → no blur at bottom edge.
                // This creates the "gradient blur effect at the bottom" the user wants.
                progressive = HazeProgressive.verticalGradient(
                    startIntensity = 1f,        // top of header = full blur
                    endIntensity = 0f,          // bottom edge of header = no blur
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
