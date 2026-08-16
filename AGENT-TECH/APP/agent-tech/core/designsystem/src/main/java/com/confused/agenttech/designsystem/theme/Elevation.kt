package com.confused.agenttech.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation tokens — used sparingly. Per DESIGN-LANGUAGE.md §5, depth comes
 * primarily from translucent glass layers + borders, NOT Material shadows.
 * Only the floating bottom nav and modal sheets get shadow elevation.
 */
@Immutable
data class AgentElevation(
    val none: Dp = 0.dp,
    val bottomNav: Dp = 8.dp,
    val modal: Dp = 16.dp,
)

@Stable
val DefaultElevation = AgentElevation()
