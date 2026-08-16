package com.confused.onlylist.ui.screens.settings

import androidx.compose.foundation.BasicText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.GlassCard
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

    val sections = listOf(
        SettingsSection("Account", listOf(
            SettingItem("AniList Account", "Not linked — tap to connect", "account"),
            SettingItem("Profile", "View your stats", "profile"),
        )),
        SettingsSection("Appearance", listOf(
            SettingItem("Theme", "Midnight Coral", "theme"),
            SettingItem("Accent Color", "Coral (#FF6B5C)", "accent"),
        )),
        SettingsSection("Data", listOf(
            SettingItem("Backup & Restore", "Not configured", "backup"),
            SettingItem("Cache", "Auto-managed", "cache"),
        )),
        SettingsSection("Agent", listOf(
            SettingItem("AI Design Agent", "Coming in Phase 4", "agent"),
            SettingItem("LLM Provider", "Not configured", "llm"),
        )),
        SettingsSection("About", listOf(
            SettingItem("Version", "Only-List v0.1.0", "version"),
            SettingItem("Open Source Licenses", "OFL fonts, Apache 2.0", "licenses"),
        )),
    )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 110.dp, bottom = 100.dp),
        ) {
            sections.forEach { section ->
                item {
                    BasicText(
                        text = section.title,
                        style = typography.titleMedium.copy(color = colors.textSecondary),
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    )
                }
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        section.items.forEachIndexed { index, item ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .pressScale { /* Phase 2: navigate to setting detail */ }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    BasicText(
                                        text = item.title,
                                        style = typography.titleMedium.copy(color = colors.textPrimary),
                                    )
                                    BasicText(
                                        text = item.subtitle,
                                        style = typography.bodySmall.copy(color = colors.textTertiary),
                                    )
                                }
                                BasicText(
                                    text = "›",
                                    style = typography.titleLarge.copy(color = colors.textTertiary),
                                )
                            }
                            if (index < section.items.size - 1) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.outline.copy(alpha = 0.3f)),
                                )
                            }
                        }
                    }
                }
            }
        }
        CollapsibleHeader(title = "Settings", listState = listState)
    }
}

private data class SettingsSection(val title: String, val items: List<SettingItem>)
private data class SettingItem(val title: String, val subtitle: String, val key: String)

