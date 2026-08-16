package com.confused.onlylist.ui.screens.logs

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.confused.onlylist.common.LogEntry
import com.confused.onlylist.common.LogLevel
import com.confused.onlylist.common.Logger
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.SegmentedControl
import com.confused.onlylist.designsystem.components.pressScale
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Logging screen — shows recent log entries with level filtering.
 * Per CORE_RULES §20: "proper console logging alongside it, with proper
 * filtering functionality for the logs."
 */
@Composable
fun LogsScreen(hazeState: HazeState) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    val allLogs by Logger.logBuffer.collectAsState()
    var selectedLevel by remember { mutableIntStateOf(0) }

    val levelOptions = listOf("All", "Info", "Warn", "Error")
    val minLevel = when (selectedLevel) {
        1 -> LogLevel.INFO
        2 -> LogLevel.WARN
        3 -> LogLevel.ERROR
        else -> LogLevel.VERBOSE
    }
    val filteredLogs = if (selectedLevel == 0) allLogs else allLogs.filter { it.level.priority >= minLevel.priority }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp),
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SegmentedControl(
                        options = levelOptions,
                        selectedIndex = selectedLevel,
                        onSelected = { selectedLevel = it },
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .pressScale { Logger.clear() }
                            .clip(shapes.medium)
                            .background(colors.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        BasicText(
                            text = "Clear",
                            style = typography.caption.copy(color = colors.textSecondary),
                        )
                    }
                }
            }

            item {
                BasicText(
                    text = "${filteredLogs.size} entries",
                    style = typography.caption.copy(color = colors.textTertiary),
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }

            items(filteredLogs) { entry ->
                LogEntryRow(entry = entry, colors = colors, typography = typography, dateFormat = dateFormat)
            }
        }
        CollapsibleHeader(title = "Logs", listState = listState, hazeState = hazeState)
    }
}

@Composable
private fun LogEntryRow(
    entry: LogEntry,
    colors: com.confused.onlylist.designsystem.theme.OnlyListColors,
    typography: com.confused.onlylist.designsystem.theme.OnlyListTypography,
    dateFormat: SimpleDateFormat,
) {
    val levelColor = when (entry.level) {
        LogLevel.VERBOSE -> colors.textTertiary
        LogLevel.DEBUG -> colors.info
        LogLevel.INFO -> colors.textSecondary
        LogLevel.WARN -> colors.warning
        LogLevel.ERROR -> colors.error
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = entry.level.label,
                style = typography.caption.copy(color = levelColor, fontFamily = FontFamily.Monospace),
            )
            BasicText(
                text = dateFormat.format(Date(entry.timestamp)),
                style = typography.caption.copy(color = colors.textTertiary, fontFamily = FontFamily.Monospace),
            )
            BasicText(
                text = entry.tag,
                style = typography.caption.copy(color = colors.info, fontFamily = FontFamily.Monospace),
            )
        }
        BasicText(
            text = entry.message,
            style = typography.bodySmall.copy(color = colors.textPrimary, fontFamily = FontFamily.Monospace),
        )
        if (entry.stackTrace != null) {
            BasicText(
                text = entry.stackTrace.take(200),
                style = typography.bodySmall.copy(color = colors.error, fontFamily = FontFamily.Monospace),
            )
        }
    }
}
