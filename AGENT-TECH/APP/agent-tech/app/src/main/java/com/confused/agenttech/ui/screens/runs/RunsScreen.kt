package com.confused.agenttech.ui.screens.runs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.confused.agenttech.AppContainer
import com.confused.agenttech.database.entity.SessionEntity
import com.confused.agenttech.designsystem.components.CollapsibleHeader
import com.confused.agenttech.designsystem.components.GlassCard
import com.confused.agenttech.designsystem.components.SegmentedControl
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalShapes
import com.confused.agenttech.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RunsScreen(hazeState: HazeState) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current

    val sessions by AppContainer.sessionRepository.observeAll().collectAsState(initial = emptyList())
    var selectedSegment by remember { mutableIntStateOf(0) }

    val filtered = when (selectedSegment) {
        1 -> sessions.filter { it.status == "success" }
        2 -> sessions.filter { it.status == "error" }
        3 -> sessions.filter { it.status == "running" }
        else -> sessions
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp),
        ) {
            item {
                SegmentedControl(
                    options = listOf("All", "Success", "Error", "Running"),
                    selectedIndex = selectedSegment,
                    onSelected = { selectedSegment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "No runs yet. Start a conversation in Chat.",
                            style = typography.bodyMedium.copy(color = colors.textTertiary),
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { session ->
                    RunRow(session)
                }
            }
        }
        CollapsibleHeader(title = "Runs", listState = listState, hazeState = hazeState)
    }
}

@Composable
private fun RunRow(session: SessionEntity) {
    val colors = LocalColors.current
    val typography = LocalTypography.current
    val shapes = LocalShapes.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Status dot
            val dotColor = when (session.status) {
                "running" -> colors.yellow
                "success" -> colors.success
                "error" -> colors.error
                "stopped" -> colors.textTertiary
                else -> colors.info
            }
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = session.title.ifBlank { "(untitled run)" },
                    style = typography.titleMedium.copy(color = colors.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                        .format(Date(session.createdAt)),
                    style = typography.caption.copy(color = colors.textTertiary),
                )
                if (session.inputTokens + session.outputTokens > 0) {
                    BasicText(
                        text = "${session.inputTokens + session.outputTokens} tokens",
                        style = typography.caption.copy(color = colors.textSecondary),
                    )
                }
            }
        }
    }
}
