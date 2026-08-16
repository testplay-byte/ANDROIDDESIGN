package com.confused.agenttech.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalElevation
import com.confused.agenttech.designsystem.theme.LocalMotion
import com.confused.agenttech.designsystem.theme.LocalShapes
import com.confused.agenttech.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

// ── Floating pill bottom navigation — DARK GREY frosted glass counterpoint ──
// Per SCREEN-PLAN.md: 4 tabs (Chat / Files / Runs / Settings), blue active indicator.
// Per R-13 fixes: pressScale FIRST (outermost), every item weight(1f), no spacedBy gaps,
// OPAQUE backgroundColor for blur (dark grey #2E2E2E).

@Stable
data class BottomNavItem(
    val route: String,
    val iconRes: Int,
    val label: String,
)

@Composable
fun AgentBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem>,
) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val elevation = LocalElevation.current

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    elevation = elevation.bottomNav,
                    shape = shapes.pill,
                    ambientColor = colors.surfaceDark.copy(alpha = 0.4f),
                    spotColor = colors.surfaceDark.copy(alpha = 0.5f),
                )
                .clip(shapes.pill)
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        // OPAQUE backing — fixes text not being frosted (per R-13).
                        backgroundColor = colors.surfaceDark,
                        blurRadius = 24.dp,
                        tints = listOf(HazeTint(colors.surfaceDark.copy(alpha = 0.80f))),
                    ),
                )
                .height(58.dp)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isActive = currentRoute == item.route
                BottomNavItemView(
                    item = item,
                    isActive = isActive,
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavItemView(
    item: BottomNavItem,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current
    val motion = LocalMotion.current

    Box(
        modifier = modifier
            .height(42.dp)
            // R-13 FIX: pressScale FIRST (outermost) → full slot is tappable
            .pressScale(pressedScale = 0.95f, onClick = onClick)
            .then(
                if (isActive) {
                    Modifier
                        .clip(shapes.pill)
                        .background(colors.blue.copy(alpha = 0.18f))
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = if (isActive) 14.dp else 10.dp),
        ) {
            Image(
                painter = painterResource(item.iconRes),
                contentDescription = item.label,
                colorFilter = ColorFilter.tint(
                    if (isActive) colors.blue else colors.textOnDarkTertiary
                ),
                modifier = Modifier.size(22.dp),
            )
            AnimatedVisibility(
                visible = isActive,
                enter = expandHorizontally(
                    animationSpec = tween(motion.shortMs),
                    expandFrom = Alignment.Start,
                ) + fadeIn(animationSpec = tween(motion.quickMs)),
                exit = fadeOut(animationSpec = tween(100)) +
                        shrinkHorizontally(animationSpec = tween(150)),
            ) {
                BasicText(
                    text = item.label,
                    style = typography.titleMedium.copy(
                        color = if (isActive) colors.blue else colors.textOnDarkTertiary
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}
