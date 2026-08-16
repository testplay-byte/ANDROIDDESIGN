package com.confused.onlylist.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── CompositionLocals — the 5 token sets the UI reads (NOT Material's MaterialTheme) ──
// Elevation is intentionally absent: depth comes from translucent layers + borders + scrim,
// not a Material shadow ramp. (DESIGN-LANGUAGE.md §5.)

val LocalColors = staticCompositionLocalOf { MidnightCoralColors }
val LocalTypography = staticCompositionLocalOf { DefaultTypography }
val LocalShapes = staticCompositionLocalOf { DefaultShapes }
val LocalMotion = staticCompositionLocalOf { DefaultMotion }
val LocalSpacing = staticCompositionLocalOf { DefaultSpacing }

/**
 * AppTheme — the ONLY way to theme the app. NOT Material.
 * Provides 5 CompositionLocals (colors / typography / shapes / motion / spacing).
 *
 * v1 hardcodes the Midnight Coral palette. Phase 4 will accept a [DesignTokens] param
 * sourced from theme.json (the AI agent's edit surface), with a StateFlow-driven swap.
 */
@Composable
fun AppTheme(
    colors: OnlyListColors = MidnightCoralColors,
    typography: OnlyListTypography = DefaultTypography,
    shapes: OnlyListShapes = DefaultShapes,
    motion: OnlyListMotion = DefaultMotion,
    spacing: OnlyListSpacing = DefaultSpacing,
    content: @Composable () -> Unit,
) {
    val rememberedColors = remember(colors) { colors }
    val rememberedTypography = remember(typography) { typography }
    val rememberedShapes = remember(shapes) { shapes }
    val rememberedMotion = remember(motion) { motion }
    val rememberedSpacing = remember(spacing) { spacing }

    // Edge-to-edge + status bar color matches the warm dark bg.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(
        LocalColors provides rememberedColors,
        LocalTypography provides rememberedTypography,
        LocalShapes provides rememberedShapes,
        LocalMotion provides rememberedMotion,
        LocalSpacing provides rememberedSpacing,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            content()
        }
    }
}
