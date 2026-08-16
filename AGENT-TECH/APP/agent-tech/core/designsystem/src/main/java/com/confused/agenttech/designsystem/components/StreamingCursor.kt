package com.confused.agenttech.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.confused.agenttech.designsystem.theme.LocalColors

// ── StreamingCursor — per DESIGN-LANGUAGE.md §4 ──
//
// Thin vertical blue bar, opacity 1.0 ↔ 0.0 over 800ms (linear, infinite).
// Appended at the end of a streaming assistant message; removed when the
// finish_reason is set.

@Composable
fun StreamingCursor(modifier: Modifier = Modifier) {
    val colors = LocalColors.current
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )
    Box(
        modifier
            .width(2.dp)
            .height(16.dp)
            .background(colors.blue.copy(alpha = alpha))
    )
}
