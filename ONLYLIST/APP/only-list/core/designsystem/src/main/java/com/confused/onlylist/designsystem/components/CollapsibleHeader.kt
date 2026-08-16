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
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.dp
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

// ── Collapsible header with frosted glass + scroll-driven title shrink ──
// Per DESIGN-LANGUAGE.md §7.2 + R-9 research.
//
// Key fixes (vs the old version):
// - Title SHRINKS (displayLarge 30sp → titleLarge 18sp) + moves up (padding 8→2 / 4→0)
//   based on scroll, using lerp() + a single animateFloatAsState(spring).
// - Scrim is a REAL frosted blur (Haze) not a gradient — fixes the "no top background" complaint.
// - No shadow, no gradient seam — fixes the "line" complaint.

@Composable
fun CollapsibleHeader(
    title: String,
    listState: LazyListState,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColors.current
    val typography = LocalTypography.current

    // Scroll delta. Use MAX when firstVisibleItemIndex > 0 so the header
    // stays fully collapsed once you scroll past the first item.
    val rawOffset = if (listState.firstVisibleItemIndex == 0) {
        listState.firstVisibleItemScrollOffset.toFloat()
    } else {
        Float.MAX_VALUE
    }
    val collapseFraction = (rawOffset / 200f).coerceIn(0f, 1f)

    // Single source of truth for animation timing — one spring drives everything.
    val animatedFraction by animateFloatAsState(
        targetValue = collapseFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "headerCollapse",
    )

    // Scrim: 0 → 0.85 alpha, frosted (Haze-backed).
    // At scroll=0: transparent (you see the content behind). On scroll: frosted glass fades in.
    val scrimAlpha = animatedFraction * 0.85f

    // Title style: lerp(large → small).
    val animatedStyle = lerp(typography.displayLarge, typography.titleLarge, animatedFraction)

    // Padding: 8→2 top, 4→0 bottom.
    val topPad = androidx.compose.ui.unit.lerp(8.dp, 2.dp, animatedFraction)
    val bottomPad = androidx.compose.ui.unit.lerp(4.dp, 0.dp, animatedFraction)

    Box(modifier.fillMaxWidth()) {
        // Layer 1 (bottom): frosted scrim — fills behind title + status bar.
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        tint = HazeTint(colors.surface.copy(alpha = scrimAlpha)),
                        blurRadius = 20.dp,
                    ),
                )
        )
        // Layer 2 (top): title — on top of the scrim.
        BasicText(
            text = title,
            style = animatedStyle.copy(color = colors.textPrimary),
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = topPad, bottom = bottomPad),
        )
    }
}
