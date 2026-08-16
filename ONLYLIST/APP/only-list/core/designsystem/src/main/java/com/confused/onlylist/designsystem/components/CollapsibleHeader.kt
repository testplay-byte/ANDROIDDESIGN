package com.confused.onlylist.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

// ── Collapsible header per user spec (R-11 revision) ──
//
// Requirements (from user feedback):
// 1. Title is 1.5x the current size (was 30sp → now 45sp displayLarge, lerp → 24sp).
// 2. Title STAYS BOLD always — don't lerp the fontWeight (was causing the
//    "bold → normal → bold again" flicker the user saw).
// 3. The whole top section has a background of the app's background color
//    (always present, behind the status bar + title).
// 4. A gradient blur / darkening effect transitions in on scroll (frosted
//    glass that gets stronger as you scroll down).

@Composable
fun CollapsibleHeader(
    title: String,
    listState: LazyListState,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColors.current
    val typography = LocalTypography.current

    // Scroll-driven collapse fraction: 0 (top) → 1 (scrolled 200dp).
    val rawOffset = if (listState.firstVisibleItemIndex == 0) {
        listState.firstVisibleItemScrollOffset.toFloat()
    } else {
        Float.MAX_VALUE
    }
    val collapseFraction = (rawOffset / 200f).coerceIn(0f, 1f)

    // Single spring drives everything — smooth, no flicker between weights.
    val animatedFraction by animateFloatAsState(
        targetValue = collapseFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "headerCollapse",
    )

    // Title size: lerp from 45sp (displayLarge) → 24sp. Weight STAYS Bold.
    val titleFontSize = lerp(45.sp, 24.sp, animatedFraction)
    val titleStyle = typography.displayLarge.copy(
        fontSize = titleFontSize,
        fontWeight = FontWeight.Bold,  // always bold — no weight lerp
        color = colors.textPrimary,
    )

    // Padding: 8→2 top, 4→0 bottom.
    val topPad = lerp(8.dp, 2.dp, animatedFraction)
    val bottomPad = lerp(4.dp, 0.dp, animatedFraction)

    // Frosted glass tint: 0 (transparent at top) → 0.85 (frosted when scrolled).
    // At scroll=0: content scrolls behind the transparent header (you see it through).
    // On scroll: the frosted glass fades in, blurring content behind.
    val scrimAlpha = animatedFraction * 0.85f

    Box(
        modifier
            .fillMaxWidth()
            // Always-present background: the app's bg color, so the header area
            // is never transparent at the very top (avoids content showing through
            // the status bar region awkwardly).
            .background(colors.background)
    ) {
        // Layer 1 (bottom): frosted glass scrim — fades in on scroll.
        // Reads from the same hazeState as the LazyColumn (the blur source).
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = Color.Transparent,
                        blurRadius = 24.dp,
                        tints = listOf(HazeTint(colors.surface.copy(alpha = scrimAlpha))),
                    ),
                )
        )

        // Layer 2 (top): title — always bold, size animates on scroll.
        BasicText(
            text = title,
            style = titleStyle,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = topPad, bottom = bottomPad),
        )
    }
}
