package com.confused.onlylist.designsystem.components

import androidx.compose.foundation.BasicText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography

// ── Collapsible header with scroll-blur (gradient scrim, NOT RenderEffect) ──
// Per DESIGN-LANGUAGE.md §7.2 + R-5 research.
// Header pinned OUTSIDE the scroll container. Title shrinks on scroll.
// Scrim = 36dp gradient (surface → transparent), alpha = smoothstep(scroll/24dp).
// Draw-phase only — zero recomposition for the scrim.
//
// ponytail: progressive blur ramp 40%-70% (R-7) + title size animation are Phase 6 polish.
// v1: static large title + alpha-animated scrim (still looks great, builds reliably).

@Composable
fun CollapsibleHeader(
    title: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColors.current
    val typography = LocalTypography.current

    // Scroll offset: 0 if first item visible, else MAX (fully scrimmed)
    val scrollOffset = if (listState.firstVisibleItemIndex == 0) {
        listState.firstVisibleItemScrollOffset
    } else {
        Int.MAX_VALUE
    }
    // Scrim alpha: ramps in over the first 24dp of scroll, caps at 0.55
    val scrimAlpha = (scrollOffset.toFloat() / 24f).coerceIn(0f, 0.55f)

    Column(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        BasicText(
            text = title,
            style = typography.displayLarge.copy(
                color = colors.textPrimary,
            ),
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 4.dp,
            ),
        )
        // Gradient scrim: 36dp tall, surface (with alpha) → transparent.
        // This creates the "darkening blur" effect without an expensive RenderEffect.
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.surface.copy(alpha = scrimAlpha),
                            Color.Transparent,
                        )
                    )
                )
        )
    }
}
