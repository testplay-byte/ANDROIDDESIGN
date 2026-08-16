package com.confused.agenttech.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

// ── Primary Glass palette — see APP/agent-tech/DESIGN-LANGUAGE.md §1 ──
//
// Light-first palette: cool neutral grey background + three primaries
// (red / yellow / blue) + dark-grey counterpoint. NOT Material You.

@Immutable
data class AgentColors(
    // Backgrounds (light)
    val background: Color = Color(0xFFF5F5F5),
    val surface: Color = Color(0xFFFFFFFF),
    val surfaceVariant: Color = Color(0xFFEAEAEA),

    // Dark grey counterpoint
    val surfaceDark: Color = Color(0xFF2E2E2E),
    val surfaceDarkest: Color = Color(0xFF1F1F1F),

    // Borders / dividers
    val outline: Color = Color(0xFFD4D4D4),
    val outlineDark: Color = Color(0xFF3A3A3A),
    val divider: Color = Color(0xFFEEEEEE),

    // Primaries (Material-600 shades — tuned to coexist on #F5F5F5)
    val red: Color = Color(0xFFE53935),
    val redHover: Color = Color(0xFFEF5350),
    val redPressed: Color = Color(0xFFC62828),
    val redMuted: Color = Color(0xFFFFEBEE),

    val yellow: Color = Color(0xFFFFC107),
    val yellowHover: Color = Color(0xFFFFD54F),
    val yellowPressed: Color = Color(0xFFFF8F00),
    val yellowMuted: Color = Color(0xFFFFF8E1),

    val blue: Color = Color(0xFF1E88E5),
    val blueHover: Color = Color(0xFF42A5F5),
    val bluePressed: Color = Color(0xFF1565C0),
    val blueMuted: Color = Color(0xFFE3F2FD),

    // Semantic
    val success: Color = Color(0xFF10B981),
    val warning: Color = Color(0xFFF59E0B),
    val error: Color = Color(0xFFE53935),
    val info: Color = Color(0xFF0EA5E9),

    // Text on light
    val textPrimary: Color = Color(0xFF1F1F1F),
    val textSecondary: Color = Color(0xFF525252),
    val textTertiary: Color = Color(0xFFA3A3A3),
    val textDisabled: Color = Color(0xFFB8B8B8),

    // Text on dark grey
    val textOnDark: Color = Color(0xFFF5F5F5),
    val textOnDarkSecondary: Color = Color(0xFFB0B0B0),
    val textOnDarkTertiary: Color = Color(0xFF8A8A8A),

    // Code syntax (on #1F1F1F)
    val codeKeyword: Color = Color(0xFFFF7B72),
    val codeFunction: Color = Color(0xFFD2A8FF),
    val codeString: Color = Color(0xFFA5D6FF),
    val codeNumber: Color = Color(0xFF79C0FF),
    val codeComment: Color = Color(0xFF8B949E),
    val codeType: Color = Color(0xFFFFA657),
    val codeVariable: Color = Color(0xFFFFA657),
    val codePlain: Color = Color(0xFFE6EDF3),
    val codeAddedBg: Color = Color(0xFF1B3A20),
    val codeAddedText: Color = Color(0xFF7EE787),
    val codeRemovedBg: Color = Color(0xFF4A1F1F),
    val codeRemovedText: Color = Color(0xFFFF7B72),
)

@Stable
val PrimaryGlassColors = AgentColors()
