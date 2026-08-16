package com.confused.agenttech.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalShapes

/**
 * A frosted-glass card — opaque surface + thin outline border (no shadow).
 *
 * Per DESIGN-LANGUAGE.md §7 ("Where to use glass"): cards are OPAQUE, not glass.
 * Glass is reserved for overlays (nav, headers, sheets, FABs).
 * This component is the primary "elevated card" surface — surfaceVariant-free,
 * just surface + outline + radius.
 *
 * (Named "GlassCard" for consistency with Only-List naming, but per the design
 *  language it's an opaque surface card. Real Haze glass is in the overlay
 *  components: BottomBar / CollapsibleHeader / input bars.)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current

    Column(
        modifier = modifier
            .clip(shapes.large)
            .background(colors.surface)
            .border(0.5.dp, colors.outline, shapes.large)
            .padding(16.dp),
        content = content,
    )
}
