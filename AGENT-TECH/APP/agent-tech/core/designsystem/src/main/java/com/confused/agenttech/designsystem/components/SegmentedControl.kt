package com.confused.agenttech.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalShapes
import com.confused.agenttech.designsystem.theme.LocalTypography

// ── 3-way segmented control — per DESIGN-LANGUAGE.md §7.5 ──
// pill container, surfaceVariant bg, selected segment = blueMuted pill + blue text.

/**
 * @param options 2-4 options to toggle between
 * @param selectedIndex current selection
 * @param onSelected callback with new index
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    Row(
        modifier = modifier
            .clip(shapes.pill)
            .background(colors.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shapes.pill)
                    .then(
                        if (isSelected) Modifier.background(colors.blueMuted)
                        else Modifier
                    )
                    .pressScale(onClick = { onSelected(index) })
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = label,
                    style = typography.caption.copy(
                        color = if (isSelected) colors.bluePressed else colors.textSecondary,
                    ),
                )
            }
        }
    }
}
