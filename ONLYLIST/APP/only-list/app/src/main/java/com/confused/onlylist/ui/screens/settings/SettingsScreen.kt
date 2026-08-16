package com.confused.onlylist.ui.screens.settings

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.pressScale
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography

@Composable
fun SettingsScreen() {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    val settings = listOf(
        "AniList Account" to "Not linked",
        "Theme" to "Midnight Coral",
        "Backup & Restore" to "Not configured",
        "AI Agent" to "Coming in Phase 4",
        "Notifications" to "Off",
        "About" to "Only-List v0.1.0",
    )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 110.dp, bottom = 100.dp),
        ) {
            items(settings.size) { index ->
                val (title, subtitle) = settings[index]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(shapes.large)
                        .background(colors.surface)
                        .pressScale { /* Phase 2: navigate to setting detail */ }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column {
                        BasicText(
                            text = title,
                            style = typography.titleMedium.copy(color = colors.textPrimary),
                        )
                        BasicText(
                            text = subtitle,
                            style = typography.bodySmall.copy(color = colors.textTertiary),
                        )
                    }
                    BasicText(
                        text = "›",
                        style = typography.titleLarge.copy(color = colors.textTertiary),
                    )
                }
            }
        }
        CollapsibleHeader(title = "Settings", listState = listState)
    }
}
