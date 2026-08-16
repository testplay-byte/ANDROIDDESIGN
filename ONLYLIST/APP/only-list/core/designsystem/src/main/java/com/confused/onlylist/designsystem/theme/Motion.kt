package com.confused.onlylist.designsystem.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

// ── Motion — see APP/only-list/DESIGN-LANGUAGE.md §4 ──

@Immutable
data class OnlyListMotion(
    val instantMs: Int = 50,
    val quickMs: Int = 150,
    val shortMs: Int = 220,
    val mediumMs: Int = 300,
    val longMs: Int = 450,

    val standardEasing: Easing = FastOutSlowInEasing,             // 0.4, 0.0, 0.2, 1.0
    val standardDecelEasing: Easing = LinearOutSlowInEasing,     // 0.0, 0.0, 0.2, 1.0
    val standardAccelerateEasing: Easing = FastOutLinearInEasing, // 0.4, 0.0, 1.0, 1.0

    val springDefault: Float = Spring.DampingRatioMediumBouncy,   // ~0.5
    val springDefaultStiffness: Float = Spring.StiffnessMedium,    // ~1500
    val springBouncy: Float = Spring.DampingRatioLowBouncy,         // ~0.75 (slight overshoot)
    val springBouncyStiffness: Float = 300f,
)

@Stable
val DefaultMotion = OnlyListMotion()
