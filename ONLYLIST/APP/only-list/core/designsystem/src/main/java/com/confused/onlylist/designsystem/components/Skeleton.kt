package com.confused.onlylist.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.confused.onlylist.designsystem.theme.LocalColors

/**
 * A skeleton placeholder block with a shimmer sweep.
 * Per DESIGN-LANGUAGE.md §7.6: gradient sweep surfaceVariant → surfaceHighest → surfaceVariant,
 * 1200ms loop, LinearEasing. Used ONLY on first-ever empty-cache load.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadiusDp: Dp = 8.dp,
) {
    val colors = LocalColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )
    val shimmerColors = listOf(
        colors.surfaceVariant,
        colors.surfaceHighest,
        colors.surfaceVariant,
    )
    Box(
        modifier
            .then(
                if (cornerRadiusDp > 0.dp) {
                    Modifier.clip(RoundedCornerShape(cornerRadiusDp))
                } else {
                    Modifier  // skip internal clip when 0 — let the caller's external clip apply
                }
            )
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(translate * 300f, 0f),
                    end = Offset(translate * 300f + 300f, 100f),
                ),
            ),
    )
}

/** Convenience: skeleton shaped like a list-item row (cover + two text lines). */
@Composable
fun SkeletonListItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBox(
            modifier = Modifier.size(64.dp, 90.dp),
            cornerRadiusDp = 8.dp,
        )
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp))
        }
    }
}
