package com.confused.agenttech.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.dp

// ── Shapes — see APP/agent-tech/DESIGN-LANGUAGE.md §3 ──

@Immutable
data class AgentShapes(
    val small: RoundedCornerShape = RoundedCornerShape(4.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(8.dp),
    val large: RoundedCornerShape = RoundedCornerShape(12.dp),
    val xlarge: RoundedCornerShape = RoundedCornerShape(20.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(28.dp),
    val codeBlock: RoundedCornerShape = RoundedCornerShape(8.dp),
)

@Stable
val DefaultShapes = AgentShapes()
