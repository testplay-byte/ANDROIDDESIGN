package com.confused.agenttech.designsystem.theme

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

// ── CompositionLocals — the 6 token sets the UI reads (NOT Material's MaterialTheme) ──
//
// Elevation is intentionally absent: depth comes from translucent layers + borders +
// scrim, not a Material shadow ramp. (DESIGN-LANGUAGE.md §5.)

val LocalColors = staticCompositionLocalOf { PrimaryGlassColors }
val LocalTypography = staticCompositionLocalOf { DefaultTypography }
val LocalShapes = staticCompositionLocalOf { DefaultShapes }
val LocalMotion = staticCompositionLocalOf { DefaultMotion }
val LocalSpacing = staticCompositionLocalOf { DefaultSpacing }
val LocalElevation = staticCompositionLocalOf { AgentElevation() }

/**
 * AppTheme — the ONLY way to theme the app. NOT Material.
 * Provides 6 CompositionLocals (colors / typography / shapes / motion / spacing / elevation).
 *
 * v1 hardcodes the Primary Glass palette (light, red/yellow/blue + dark grey counterpoint).
 */
@Composable
fun AppTheme(
    colors: AgentColors = PrimaryGlassColors,
    typography: AgentTypography = DefaultTypography,
    shapes: AgentShapes = DefaultShapes,
    motion: AgentMotion = DefaultMotion,
    spacing: AgentSpacing = DefaultSpacing,
    elevation: AgentElevation = AgentElevation(),
    content: @Composable () -> Unit,
) {
    val rememberedColors = remember(colors) { colors }
    val rememberedTypography = remember(typography) { typography }
    val rememberedShapes = remember(shapes) { shapes }
    val rememberedMotion = remember(motion) { motion }
    val rememberedSpacing = remember(spacing) { spacing }
    val rememberedElevation = remember(elevation) { elevation }

    // Edge-to-edge + transparent bars. Light status bar icons on the light bg.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    CompositionLocalProvider(
        LocalColors provides rememberedColors,
        LocalTypography provides rememberedTypography,
        LocalShapes provides rememberedShapes,
        LocalMotion provides rememberedMotion,
        LocalSpacing provides rememberedSpacing,
        LocalElevation provides rememberedElevation,
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
