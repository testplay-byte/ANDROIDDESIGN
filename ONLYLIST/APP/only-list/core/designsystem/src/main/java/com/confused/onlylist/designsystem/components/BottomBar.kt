package com.confused.onlylist.designsystem.components

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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalMotion
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

// ── Bottom navigation bar — floating pill + TRUE frosted glass (R-13 fix) ──

@Stable
data class BottomNavItem(
    val route: String,
    val iconRes: Int,
    val label: String,
)

/**
 * Floating pill bottom navigation with TRUE frosted glass.
 *
 * R-13 FIXES:
 * 1. backgroundColor = colors.surface (OPAQUE, not Transparent) — fixes
 *    "only images frosted, not text" bug. Haze needs an opaque backing for
 *    the blur kernel to smear text pixels.
 * 2. Every item gets weight(1f) — no tap-through gaps between buttons.
 * 3. pressScale applied BEFORE visual modifiers — full slot is tappable.
 * 4. No spacedBy — items butt against each other.
 * 5. blurRadius = 28.dp for a light frosted feel.
 */
@Composable
fun OnlyListBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem> = defaultBottomNavItems(),
) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current

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
                .clip(shapes.pill)
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        // R-13 FIX: OPAQUE backing (was Color.Transparent) —
                        // fixes text not being frosted.
                        backgroundColor = colors.surface,
                        blurRadius = 28.dp,
                        tints = listOf(HazeTint(colors.surface.copy(alpha = 0.6f))),
                    ),
                )
                .height(58.dp)
                .padding(8.dp),
            // R-13 FIX: no spacedBy — items butt against each other (no tap-through gaps)
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isActive = currentRoute == item.route
                // R-13 FIX: every item gets weight(1f) — equal slot, no gaps
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
            // Visual styling AFTER clickable
            .then(
                if (isActive) {
                    Modifier
                        .clip(shapes.pill)
                        .background(colors.primaryMuted.copy(alpha = 0.8f))
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
                    if (isActive) colors.primary else colors.textTertiary
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
                        color = if (isActive) colors.primary else colors.textTertiary
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun defaultBottomNavItems(): List<BottomNavItem> = listOf(
    BottomNavItem("home", com.confused.onlylist.designsystem.R.drawable.ic_home, "Home"),
    BottomNavItem("search", com.confused.onlylist.designsystem.R.drawable.ic_search, "Search"),
    BottomNavItem("airing", com.confused.onlylist.designsystem.R.drawable.ic_calendar, "Airing"),
    BottomNavItem("library", com.confused.onlylist.designsystem.R.drawable.ic_library, "Library"),
    BottomNavItem("settings", com.confused.onlylist.designsystem.R.drawable.ic_settings, "Settings"),
)
