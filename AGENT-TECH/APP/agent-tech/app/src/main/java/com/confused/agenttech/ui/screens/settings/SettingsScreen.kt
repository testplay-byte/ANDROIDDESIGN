package com.confused.agenttech.ui.screens.settings

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.confused.agenttech.AppContainer
import com.confused.agenttech.designsystem.components.CollapsibleHeader
import com.confused.agenttech.designsystem.components.GlassCard
import com.confused.agenttech.designsystem.components.pressScale
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun SettingsScreen(
    hazeState: HazeState,
    onNavigateToProviderConfig: () -> Unit,
    onNavigateToUsage: () -> Unit,
) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current

    val providers by AppContainer.providerRepository.observeAll().collectAsState(initial = emptyList())
    val activeProvider by AppContainer.providerRepository.observeActive().collectAsState(initial = null)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp),
        ) {
            // LLM Providers section
            item {
                SectionHeader("LLM Providers")
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    providers.forEachIndexed { index, provider ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .pressScale { onNavigateToProviderConfig() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                BasicText(
                                    text = provider.name,
                                    style = typography.titleMedium.copy(color = colors.textPrimary),
                                )
                                BasicText(
                                    text = provider.modelName,
                                    style = typography.bodySmall.copy(color = colors.textTertiary),
                                )
                            }
                            BasicText(
                                text = if (provider.isActive) "● active" else if (provider.apiKey.isBlank()) "configure" else "tap to activate",
                                style = typography.caption.copy(
                                    color = if (provider.isActive) colors.success else colors.textTertiary
                                ),
                            )
                        }
                        if (index < providers.size - 1) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.divider),
                            )
                        }
                    }
                }
            }

            // Active Model
            item {
                SectionHeader("Active Model")
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    BasicText(
                        text = activeProvider?.let { "${it.name} / ${it.modelName}" }
                            ?: "Not configured",
                        style = typography.titleMedium.copy(color = colors.textPrimary),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            // API Keys — same list as providers, but with a "view" affordance.
            item {
                SectionHeader("API Keys")
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    providers.forEach { provider ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .pressScale { onNavigateToProviderConfig() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicText(
                                text = provider.name,
                                style = typography.bodyMedium.copy(color = colors.textPrimary),
                                modifier = Modifier.weight(1f),
                            )
                            BasicText(
                                text = if (provider.apiKey.isBlank()) "—" else "•••• ${provider.apiKey.takeLast(4)}",
                                style = typography.codeInline.copy(color = colors.textSecondary),
                            )
                        }
                    }
                }
            }

            // Auto-Approve
            item {
                SectionHeader("Agent")
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            BasicText(
                                text = "Auto-Approve",
                                style = typography.titleMedium.copy(color = colors.textPrimary),
                            )
                            BasicText(
                                text = "All actions run in the dedicated project folder.",
                                style = typography.bodySmall.copy(color = colors.textTertiary),
                            )
                        }
                        BasicText(
                            text = "ON",
                            style = typography.caption.copy(color = colors.success),
                        )
                    }
                }
            }

            // Usage & Limits
            item {
                SectionHeader("Usage & Limits")
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pressScale { onNavigateToUsage() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = "View usage & set caps",
                            style = typography.titleMedium.copy(color = colors.textPrimary),
                            modifier = Modifier.weight(1f),
                        )
                        BasicText(
                            text = "›",
                            style = typography.titleLarge.copy(color = colors.textTertiary),
                        )
                    }
                }
            }

            // About
            item {
                SectionHeader("About")
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BasicText(
                            text = "Agent Tech v0.1.0",
                            style = typography.bodyMedium.copy(color = colors.textPrimary),
                        )
                        BasicText(
                            text = "OFL fonts (Inter / Sora / JetBrains Mono). Haze 1.1.1.",
                            style = typography.bodySmall.copy(color = colors.textTertiary),
                        )
                    }
                }
            }
        }
        CollapsibleHeader(title = "Settings", listState = listState, hazeState = hazeState)
    }
}

@Composable
private fun SectionHeader(title: String) {
    val typography = LocalTypography.current
    val colors = LocalColors.current
    BasicText(
        text = title,
        style = typography.titleMedium.copy(color = colors.textSecondary),
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}
