package com.confused.onlylist.ui.screens.search

import androidx.compose.foundation.text.BasicText
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.confused.onlylist.data.mock.MockData
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.SegmentedControl
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalShapes
import com.confused.onlylist.designsystem.theme.LocalTypography
import com.confused.onlylist.ui.components.MediaCard

@Composable
fun SearchScreen() {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current
    var selectedSegment by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }

    val results = if (query.isBlank()) {
        MockData.trending + MockData.completed
    } else {
        (MockData.trending + MockData.completed).filter {
            it.title.contains(query, ignoreCase = true) ||
            it.titleEnglish.contains(query, ignoreCase = true)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 110.dp, bottom = 100.dp),
        ) {
            // Search field
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(48.dp)
                        .clip(shapes.medium)
                        .background(colors.surfaceVariant)
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.textPrimary),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        decorationBox = { innerTextField ->
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (query.isEmpty()) {
                                    BasicText(
                                        text = "Search anime or manga...",
                                        style = typography.bodyMedium.copy(color = colors.textTertiary),
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
            // Anime/Manga toggle
            item {
                SegmentedControl(
                    options = listOf("Anime", "Manga"),
                    selectedIndex = selectedSegment,
                    onSelected = { selectedSegment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            // Results (2-column grid)
            item {
                BasicText(
                    text = "${results.size} results",
                    style = typography.caption.copy(color = colors.textTertiary),
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }
            // Grid rows (2 per row)
            val rowCount = (results.size + 1) / 2
            for (rowIndex in 0 until rowCount) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        for (colIndex in 0..1) {
                            val index = rowIndex * 2 + colIndex
                            if (index < results.size) {
                                Box(Modifier.weight(1f)) {
                                    MediaCard(
                                        media = results[index],
                                        onClick = { /* Phase 2: navigate to details */ },
                                    )
                                }
                            } else {
                                Box(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        CollapsibleHeader(title = "Search", listState = listState)
    }
}
