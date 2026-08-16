package com.confused.onlylist.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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

// ── Bottom navigation bar — floating pill + animated label reveal ──
// Per DESIGN-LANGUAGE.md §7.1.

@Stable
data class BottomNavItem(
    val route: String,
    val iconRes: Int,
    val label: String,
)

/**
 * Floating pill bottom navigation. NOT a Scaffold.bottomBar — overlays content.
 * Active tab = content-sized pill (primaryMuted bg) with icon + label.
 * Inactive = icon-only, equal-weight distribution.
 * Label reveal: expandHorizontally + fadeIn. Press: pressScale (scale 0.95, no ripple).
 */
@Composable
fun OnlyListBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
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
                .background(colors.surface.copy(alpha = 0.88f))
                .height(58.dp)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isActive = currentRoute == item.route
                val itemModifier = if (isActive) Modifier else Modifier.weight(1f)
                BottomNavItemView(
                    item = item,
                    isActive = isActive,
                    onClick = { onNavigate(item.route) },
                    modifier = itemModifier,
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

    Row(
        modifier = modifier
            .height(42.dp)
            .then(
                if (isActive) {
                    Modifier
                        .clip(shapes.pill)
                        .background(colors.primaryMuted)
                        .padding(horizontal = 14.dp)
                } else {
                    Modifier.padding(horizontal = 10.dp)
                }
            )
            .pressScale(
                pressedScale = 0.95f,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
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

// ── Default items ──
// iconRes IDs are resolved at runtime from the designsystem module's R.
// Using Int (resource ID) keeps this non-Material + works with Image(painterResource).
@Composable
private fun defaultBottomNavItems(): List<BottomNavItem> = listOf(
    BottomNavItem("home", com.confused.onlylist.designsystem.R.drawable.ic_home, "Home"),
    BottomNavItem("search", com.confused.onlylist.designsystem.R.drawable.ic_search, "Search"),
    BottomNavItem("airing", com.confused.onlylist.designsystem.R.drawable.ic_calendar, "Airing"),
    BottomNavItem("library", com.confused.onlylist.designsystem.R.drawable.ic_library, "Library"),
    BottomNavItem("settings", com.confused.onlylist.designsystem.R.drawable.ic_settings, "Settings"),
)
