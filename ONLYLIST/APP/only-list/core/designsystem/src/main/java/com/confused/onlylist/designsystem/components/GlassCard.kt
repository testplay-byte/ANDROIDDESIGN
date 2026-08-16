package com.confused.onlylist.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes

/**
 * A frosted-glass card — translucent gradient background + thin border + soft shadow.
 * Per user: "heavily frosted glass kind of vibe."
 *
 * Use for content cards on all screens.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current

    Column(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shapes.large,
                ambientColor = colors.background.copy(alpha = 0.4f),
                spotColor = colors.background.copy(alpha = 0.5f),
            )
            .clip(shapes.large)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.surfaceHighest.copy(alpha = 0.6f),
                        colors.surface.copy(alpha = 0.6f),
                    )
                )
            )
            .padding(16.dp),
        content = content,
    )
}
