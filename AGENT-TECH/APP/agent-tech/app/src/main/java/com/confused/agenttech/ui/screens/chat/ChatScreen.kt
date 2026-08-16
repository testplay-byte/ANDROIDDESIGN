package com.confused.agenttech.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.confused.agenttech.designsystem.components.ChatBubble
import com.confused.agenttech.designsystem.components.pressScale
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalShapes
import com.confused.agenttech.designsystem.theme.LocalTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@Composable
fun ChatScreen(hazeState: HazeState) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current
    val viewModel: ChatViewModel = viewModel()

    val messages by viewModel.messages.collectAsState()
    val input by viewModel.inputText.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streaming by viewModel.streamingText.collectAsState()
    val projects by viewModel.projects.collectAsState()

    val listState = rememberLazyListState()
    var inputField by remember { mutableStateOf(TextFieldValue(input)) }
    LaunchedEffect(input) {
        if (input.isEmpty() && inputField.text.isNotEmpty()) {
            inputField = TextFieldValue("")
        }
    }

    val totalCount = messages.size + if (streaming.isNotEmpty()) 1 else 0
    LaunchedEffect(totalCount) {
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Haze source: the chat thread.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState),
            contentPadding = PaddingValues(top = 96.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(role = msg.role, content = msg.content)
            }
            if (streaming.isNotEmpty() || (isStreaming && messages.lastOrNull()?.role != "assistant")) {
                item {
                    ChatBubble(
                        role = "assistant",
                        content = streaming,
                        isStreaming = isStreaming,
                    )
                }
            }
            if (messages.isEmpty() && streaming.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = if (projects.isEmpty())
                                "Open a project to start chatting."
                            else "Send a message to start the agent.",
                            style = typography.bodyMedium.copy(color = colors.textTertiary),
                        )
                    }
                }
            }
        }

        // Header — light glass with project name.
        Row(
            Modifier
                .fillMaxWidth()
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = colors.background,
                        blurRadius = 24.dp,
                        tints = listOf(HazeTint(colors.surface.copy(alpha = 0.73f))),
                    ),
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = projects.firstOrNull()?.name ?: "Agent Tech",
                style = typography.headingLarge.copy(color = colors.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isStreaming) {
                Box(
                    Modifier
                        .clip(shapes.small)
                        .background(colors.blueMuted)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    BasicText(
                        text = "RUNNING",
                        style = typography.toolLabel.copy(color = colors.bluePressed),
                    )
                }
            }
        }

        // Input bar — dark grey glass (the dark grey counterpoint).
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = colors.surfaceDark,
                        blurRadius = 24.dp,
                        tints = listOf(HazeTint(colors.surfaceDark.copy(alpha = 0.85f))),
                    ),
                )
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(shapes.medium)
                    .background(colors.surfaceDarkest)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (input.isEmpty()) {
                    BasicText(
                        text = "Message Agent Tech…",
                        style = typography.bodyLarge.copy(color = colors.textOnDarkTertiary),
                    )
                }
                BasicTextField(
                    value = inputField,
                    onValueChange = {
                        inputField = it
                        viewModel.onInputChange(it.text)
                    },
                    textStyle = typography.bodyLarge.copy(color = colors.textOnDark),
                    cursorBrush = SolidColor(colors.blue),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Send / Stop button
            Box(
                Modifier
                    .size(44.dp)
                    .clip(shapes.pill)
                    .background(if (isStreaming) colors.red else colors.blue)
                    .pressScale {
                        if (isStreaming) viewModel.stop() else viewModel.send()
                    },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = if (isStreaming) "■" else "↑",
                    style = typography.titleLarge.copy(color = colors.surface),
                )
            }
        }
    }
}
