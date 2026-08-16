package com.confused.agenttech.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.confused.agenttech.designsystem.theme.LocalMotion

/**
 * A clickable modifier that applies a spring press-scale (1.0 → 0.96 → 1.0) + NO ripple.
 *
 * Per DESIGN-LANGUAGE.md §4: every tappable gets scale feedback.
 * `indication = null` because we do our own feedback (no Material ripple).
 *
 * @param pressedScale target scale when pressed (default 0.96)
 * @param onClick the click handler
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.96f,
    onClick: () -> Unit,
): Modifier = composed {
    val motion = LocalMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = tween(
            durationMillis = motion.quickMs,
            easing = motion.standardDecelEasing,
        ),
        label = "pressScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}
