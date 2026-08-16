package com.confused.onlylist.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.confused.onlylist.data.mock.MockData
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.GlassCard
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.PaddingValues
import kotlin.math.cos
import kotlin.math.sin

/**
 * Profile screen — shows user stats with a radar/spider chart.
 * Per user request: "proper profile screen with charts and all other stuff like that."
 * v1: mock stats (Phase 3.5 will use real AniList Viewer data when authenticated).
 */
@Composable
fun ProfileScreen(hazeState: HazeState) {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current

    // Mock stats (Phase 3.5: real data from AniList Viewer query)
    val radarStats = listOf(
        "Action" to 0.85f,
        "Drama" to 0.92f,
        "Comedy" to 0.70f,
        "Fantasy" to 0.80f,
        "Sci-Fi" to 0.65f,
        "Slice of Life" to 0.55f,
    )
    val totalCount = MockData.currentlyWatching.size + MockData.completed.size
    val totalEpisodes = MockData.currentlyWatching.sumOf { it.progress } +
                       MockData.completed.sumOf { it.episodes }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().haze(hazeState),
            contentPadding = PaddingValues(top = 130.dp, bottom = 100.dp),
        ) {
            // Profile header (mock user)
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(LocalShapes.current.pill)
                                .background(colors.primaryMuted),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText(
                                text = "U",
                                style = typography.displayMedium.copy(color = colors.primary),
                            )
                        }
                        Column {
                            BasicText(
                                text = "Only-List User",
                                style = typography.titleLarge.copy(color = colors.textPrimary),
                            )
                            BasicText(
                                text = "Link your AniList account for real stats",
                                style = typography.bodySmall.copy(color = colors.textTertiary),
                            )
                        }
                    }
                }
            }

            // Quick stats row
            item {
                SectionHeader("Stats")
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatItem(label = "Total", value = totalCount.toString())
                        StatItem(label = "Episodes", value = totalEpisodes.toString())
                        StatItem(label = "Watching", value = MockData.currentlyWatching.size.toString())
                    }
                }
            }

            // Radar chart
            item {
                SectionHeader("Genre Distribution")
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    RadarChart(
                        stats = radarStats,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    )
                    // Legend below the chart
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        radarStats.forEach { (genre, score) ->
                            BasicText(
                                text = "$genre: ${(score * 100).toInt()}%",
                                style = typography.caption.copy(color = colors.textSecondary),
                            )
                        }
                    }
                }
            }

            // Top genres with bars
            item {
                SectionHeader("Top Genres")
            }
            items(radarStats.sortedByDescending { it.second }.take(3)) { (genre, score) ->
                GenreRow(genre = genre, score = score)
            }
        }
        CollapsibleHeader(title = "Profile", listState = listState, hazeState = hazeState)
    }
}

@Composable
private fun RadarChart(
    stats: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColors.current
    val angleStep = (2 * Math.PI / stats.size).toFloat()

    Canvas(modifier = modifier) {
        val canvasCenter = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension * 0.4f

        // Draw grid circles (5 levels)
        for (i in 1..5) {
            val r = radius * (i / 5f)
            val path = Path()
            for (j in stats.indices) {
                val angle = j * angleStep - (Math.PI / 2).toFloat()
                val x = canvasCenter.x + r * cos(angle)
                val y = canvasCenter.y + r * sin(angle)
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                color = colors.outline.copy(alpha = 0.3f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        // Draw axes
        for (i in stats.indices) {
            val angle = i * angleStep - (Math.PI / 2).toFloat()
            val x = canvasCenter.x + radius * cos(angle)
            val y = canvasCenter.y + radius * sin(angle)
            drawLine(
                color = colors.outline.copy(alpha = 0.2f),
                start = canvasCenter,
                end = Offset(x, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Draw data polygon
        val dataPath = Path()
        for (i in stats.indices) {
            val (_, value) = stats[i]
            val r = radius * value
            val angle = i * angleStep - (Math.PI / 2).toFloat()
            val x = canvasCenter.x + r * cos(angle)
            val y = canvasCenter.y + r * sin(angle)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(
            path = dataPath,
            color = colors.primary.copy(alpha = 0.3f),
        )
        drawPath(
            path = dataPath,
            color = colors.primary,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        // Draw data points
        for (i in stats.indices) {
            val (_, value) = stats[i]
            val r = radius * value
            val angle = i * angleStep - (Math.PI / 2).toFloat()
            val x = canvasCenter.x + r * cos(angle)
            val y = canvasCenter.y + r * sin(angle)
            drawCircle(
                color = colors.primary,
                radius = 4.dp.toPx(),
                center = Offset(x, y),
            )
        }
    }
}

@Composable
private fun GenreRow(genre: String, score: Float) {
    val colors = LocalColors.current
    val typography = LocalTypography.current
    val shapes = LocalShapes.current

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = genre,
            style = typography.bodyMedium.copy(color = colors.textPrimary),
            modifier = Modifier.width(100.dp),
        )
        // Score bar
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(shapes.small)
                .background(colors.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(score)
                    .height(6.dp)
                    .clip(shapes.small)
                    .background(colors.primary),
            )
        }
        BasicText(
            text = "${(score * 100).toInt()}%",
            style = typography.numberMedium.copy(color = colors.textSecondary),
        )
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
private fun StatItem(label: String, value: String) {
    val typography = LocalTypography.current
    val colors = LocalColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            text = value,
            style = typography.numberLarge.copy(color = colors.primary),
        )
        BasicText(
            text = label,
            style = typography.caption.copy(color = colors.textTertiary),
        )
    }
}
