package com.confused.onlylist.ui.screens.home

import androidx.compose.foundation.BasicText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.confused.onlylist.designsystem.components.CollapsibleHeader
import com.confused.onlylist.designsystem.components.SkeletonListItem
import com.confused.onlylist.designsystem.theme.LocalColors
import com.confused.onlylist.designsystem.theme.LocalTypography

@Composable
fun HomeScreen() {
    val listState = rememberLazyListState()
    val colors = LocalColors.current
    val typography = LocalTypography.current

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 110.dp, bottom = 100.dp),
        ) {
            item {
                BasicText(
                    text = "Welcome to Only-List",
                    style = typography.headingLarge.copy(color = colors.textPrimary),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                BasicText(
                    text = "Your anime & manga tracker",
                    style = typography.bodyMedium.copy(color = colors.textSecondary),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item {
                SectionHeader("Currently Watching")
            }
            items(3) {
                SkeletonListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            item {
                SectionHeader("Trending Now")
            }
            items(3) {
                SkeletonListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
        CollapsibleHeader(title = "Home", listState = listState)
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
