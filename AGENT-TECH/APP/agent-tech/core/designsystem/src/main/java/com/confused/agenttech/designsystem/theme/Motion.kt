package com.confused.agenttech.designsystem.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

// ── Motion — see APP/agent-tech/DESIGN-LANGUAGE.md §4 ──

@Immutable
data class AgentMotion(
    val instantMs: Int = 50,
    val quickMs: Int = 150,
    val shortMs: Int = 220,
    val mediumMs: Int = 300,
    val longMs: Int = 450,
    val streamingMs: Int = 800,
    val pulseMs: Int = 1200,

    val standardEasing: Easing = FastOutSlowInEasing,
    val standardDecelEasing: Easing = LinearOutSlowInEasing,
    val standardAccelerateEasing: Easing = FastOutLinearInEasing,
    val linearEasing: Easing = LinearEasing,

    val springDefaultDamping: Float = Spring.DampingRatioMediumBouncy,
    val springDefaultStiffness: Float = 380f,
    val springBouncyDamping: Float = 0.6f,
    val springBouncyStiffness: Float = 300f,
)

@Stable
val DefaultMotion = AgentMotion()
