package com.confused.onlylist.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

// ── Midnight Coral palette — see APP/only-list/DESIGN-LANGUAGE.md §1 ──

@Immutable
data class OnlyListColors(
    // Backgrounds (warm dark)
    val background: Color = Color(0xFF14110F),
    val surface: Color = Color(0xFF1C1815),
    val surfaceVariant: Color = Color(0xFF241F1B),
    val surfaceHighest: Color = Color(0xFF2E2823),
    val outline: Color = Color(0xFF3A322C),

    // Coral accent
    val primary: Color = Color(0xFFFF6B5C),
    val primaryHover: Color = Color(0xFFFF8A7C),
    val primaryPressed: Color = Color(0xFFE55648),
    val primaryMuted: Color = Color(0xFF3A2420),
    val onPrimary: Color = Color(0xFF1A0B08),

    // Text (warm off-white)
    val textPrimary: Color = Color(0xFFF5EFE9),
    val textSecondary: Color = Color(0xFFB5A89D),
    val textTertiary: Color = Color(0xFF8A7E72),
    val textDisabled: Color = Color(0xFF5A5249),

    // Semantic
    val success: Color = Color(0xFF6FCF97),
    val warning: Color = Color(0xFFF2C94C),
    val error: Color = Color(0xFFEB5757),
    val info: Color = Color(0xFF56CCF2),

    // List statuses
    val statusCurrent: Color = Color(0xFFFF6B5C),
    val statusCompleted: Color = Color(0xFF6FCF97),
    val statusPaused: Color = Color(0xFFF2C94C),
    val statusDropped: Color = Color(0xFFEB5757),
    val statusPlanning: Color = Color(0xFF56CCF2),
    val statusRepeating: Color = Color(0xFFBB6BD9),
)

@Stable
val MidnightCoralColors = OnlyListColors()
