package com.confused.agenttech.ui.screens.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.confused.agenttech.AppContainer
import com.confused.agenttech.designsystem.components.GlassCard
import com.confused.agenttech.designsystem.components.pressScale
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalShapes
import com.confused.agenttech.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UsageScreen(
    hazeState: HazeState,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current
    val shapes = LocalShapes.current

    val totalInput by AppContainer.usageRepository.observeTotalInputTokens().collectAsState(initial = 0)
    val totalOutput by AppContainer.usageRepository.observeTotalOutputTokens().collectAsState(initial = 0)
    val totalCostMicros by AppContainer.usageRepository.observeTotalCostMicros().collectAsState(initial = 0)
    val runCount by AppContainer.usageRepository.observeRunCount().collectAsState(initial = 0)
    val recent by AppContainer.usageRepository.observeRecent(50).collectAsState(initial = emptyList())
    val providers by AppContainer.providerRepository.observeAll().collectAsState(initial = emptyList())

    val totalCost = totalCostMicros / 1_000_000.0

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 80.dp, bottom = 100.dp),
        ) {
            // Summary cards
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryCard(
                        label = "Total tokens",
                        value = "${totalInput + totalOutput}",
                        sublabel = "in $totalInput / out $totalOutput",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryCard(
                        label = "Cost",
                        value = "$" + "%.4f".format(totalCost),
                        sublabel = "all-time",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryCard(
                        label = "Runs",
                        value = "$runCount",
                        sublabel = "all-time",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryCard(
                        label = "Avg / run",
                        value = if (runCount == 0) "—" else "${(totalInput + totalOutput) / runCount}",
                        sublabel = "tokens",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Per-provider breakdown
            item {
                SectionHeader("Per-Provider")
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    providers.forEach { provider ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            BasicText(
                                text = provider.name,
                                style = typography.bodyMedium.copy(color = colors.textPrimary),
                                modifier = Modifier.weight(1f),
                            )
                            BasicText(
                                text = provider.modelName,
                                style = typography.caption.copy(color = colors.textTertiary),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Limit settings (read-only placeholder for v1)
            item {
                SectionHeader("Limits")
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LimitRow("Per-run token cap", "0 (disabled)")
                        LimitRow("Per-run price cap", "0 (disabled)")
                        LimitRow("Monthly cap", "0 (disabled)")
                        LimitRow("Enable limits", "OFF")
                    }
                }
            }

            // Usage log
            item {
                SectionHeader("Usage Log")
            }
            if (recent.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "No usage yet.",
                            style = typography.bodyMedium.copy(color = colors.textTertiary),
                        )
                    }
                }
            } else {
                items(recent, key = { it.id }) { log ->
                    val provider = providers.firstOrNull { it.id == log.providerId }
                    GlassCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                BasicText(
                                    text = provider?.name ?: "Unknown provider",
                                    style = typography.bodyMedium.copy(color = colors.textPrimary),
                                )
                                BasicText(
                                    text = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
                                        .format(Date(log.timestamp)),
                                    style = typography.caption.copy(color = colors.textTertiary),
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                BasicText(
                                    text = "${log.inputTokens + log.outputTokens} tok",
                                    style = typography.codeInline.copy(color = colors.textSecondary),
                                )
                                val cost = log.costMicros / 1_000_000.0
                                if (cost > 0) {
                                    BasicText(
                                        text = "$" + "%.4f".format(cost),
                                        style = typography.caption.copy(color = colors.textTertiary),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.background)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .clip(shapes.pill)
                    .pressScale { onBack() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                BasicText(
                    text = "‹ Back",
                    style = typography.titleMedium.copy(color = colors.blue),
                )
            }
            BasicText(
                text = "Usage & Limits",
                style = typography.titleLarge.copy(color = colors.textPrimary),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    sublabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColors.current
    val typography = LocalTypography.current

    GlassCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                text = label.uppercase(),
                style = typography.micro.copy(color = colors.textTertiary),
            )
            BasicText(
                text = value,
                style = typography.displayMedium.copy(color = colors.textPrimary, fontWeight = FontWeight.Bold),
            )
            BasicText(
                text = sublabel,
                style = typography.caption.copy(color = colors.textSecondary),
            )
        }
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

@Composable
private fun LimitRow(label: String, value: String) {
    val colors = LocalColors.current
    val typography = LocalTypography.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(
            text = label,
            style = typography.bodyMedium.copy(color = colors.textPrimary),
        )
        BasicText(
            text = value,
            style = typography.bodyMedium.copy(color = colors.textSecondary),
        )
    }
}
