package com.confused.agenttech.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalShapes
import com.confused.agenttech.designsystem.theme.LocalTypography

// ── ToolCard — per DESIGN-LANGUAGE.md §8.1 (tool-call card) ──
//
// Light surface body + dark grey header (the dark grey counterpoint) + a
// status dot. 5 status states:
//   queued          — grey dot (textTertiary)
//   running         — yellow dot (yellow) + pulse animation
//   success         — green dot (success)
//   error           — red dot (error)
//   needs-approval  — amber dot (warning)

enum class ToolStatus {
    QUEUED, RUNNING, SUCCESS, ERROR, NEEDS_APPROVAL;

    val label: String get() = when (this) {
        QUEUED -> "queued"
        RUNNING -> "running"
        SUCCESS -> "success"
        ERROR -> "error"
        NEEDS_APPROVAL -> "needs-approval"
    }
}

@Composable
fun ToolCard(
    toolName: String,
    status: ToolStatus,
    modifier: Modifier = Modifier,
    summary: String = "",
    expanded: Boolean = false,
) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .border(0.5.dp, colors.outline, shapes.large)
            .background(colors.surface),
    ) {
        // Header — dark grey counterpoint
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.surfaceDark)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusDot(status)
            BasicText(
                text = toolName.uppercase(),
                style = typography.toolLabel.copy(color = colors.textOnDark),
            )
            Box(Modifier.weight(1f))
            BasicText(
                text = status.label,
                style = typography.toolLabel.copy(
                    color = when (status) {
                        ToolStatus.QUEUED -> colors.textOnDarkTertiary
                        ToolStatus.RUNNING -> colors.yellow
                        ToolStatus.SUCCESS -> colors.success
                        ToolStatus.ERROR -> colors.error
                        ToolStatus.NEEDS_APPROVAL -> colors.warning
                    }
                ),
            )
        }
        // Body
        if (summary.isNotEmpty() || expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (summary.isNotEmpty()) {
                    BasicText(
                        text = summary,
                        style = typography.bodySmall.copy(color = colors.textSecondary),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusDot(status: ToolStatus) {
    val colors = LocalColors.current
    val baseColor = when (status) {
        ToolStatus.QUEUED -> colors.textTertiary
        ToolStatus.RUNNING -> colors.yellow
        ToolStatus.SUCCESS -> colors.success
        ToolStatus.ERROR -> colors.error
        ToolStatus.NEEDS_APPROVAL -> colors.warning
    }
    // Always create the transition (Compose rule: hooks must be unconditional).
    val transition = rememberInfiniteTransition(label = "statusPulse")
    val pulse by transition.animateColor(
        initialValue = baseColor,
        targetValue = baseColor.copy(alpha = 0.4f),
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "statusPulseColor",
    )
    val color = if (status == ToolStatus.RUNNING) pulse else baseColor
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Suppress("unused")
private val DefaultToolCardColor = Color.Transparent
