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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

// ── Collapsible header per user spec ──
//
// R-12 FIX: the previous version had an inner scrim Box with Modifier.fillMaxSize()
// inside a wrap-content parent Box. fillMaxSize() caused the parent to EXPAND to
// full screen height → the opaque .background() + hazeChild covered the whole
// screen, blurring/darkening everything.
//
// FIX (Option A per R-12): apply hazeChild DIRECTLY to the outer Box. No inner
// scrim Box. No opaque .background(). The HazeStyle.backgroundColor provides
// the visual backing (drawn inside the hazeChild's clipped layer, behind the
// blurred pixels). The outer Box wraps to the title's height (no fillMaxSize).
//
// Title: 45sp → 24sp lerp on scroll. Weight STAYS Bold (no flicker).
// Scrim: 0 → 0.7 alpha (reduced from 0.85 — was too dark) on 200dp scroll.

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

    // Scrim alpha: 0 → 0.7. Reduced from 0.85 (was too dark per user feedback).
    val scrimAlpha = animatedFraction * 0.7f

    // R-12 FIX: apply hazeChild DIRECTLY to the outer Box.
    // No inner scrim Box, no opaque .background().
    // The HazeStyle.backgroundColor = colors.background provides the visual backing
    // (drawn behind the blurred pixels, inside the hazeChild's clipped region).
    // The outer Box wraps to the title's measured height (no fillMaxSize) — so the
    // blur only covers the header area, NOT the whole screen.
    Box(
        modifier
            .fillMaxWidth()
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = colors.background,
                    blurRadius = 24.dp,
                    tints = listOf(HazeTint(colors.surface.copy(alpha = scrimAlpha))),
                ),
            )
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
