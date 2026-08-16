package com.confused.agenttech.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.confused.agenttech.designsystem.theme.LocalColors
import com.confused.agenttech.designsystem.theme.LocalShapes
import com.confused.agenttech.designsystem.theme.LocalTypography

// ── ChatBubble — per DESIGN-LANGUAGE.md §8.1 ──
//
// Two flavors:
//   - User: blue (#1E88E5) opaque, right-aligned, bottom-right small (4dp) tail.
//   - Assistant: dark grey (#2E2E2E) opaque, left-aligned, bottom-left small (4dp) tail.
// Assistant bubbles optionally contain a code block (surfaceDarkest inset).
// Streaming state appends a StreamingCursor to the text.

/**
 * A single chat message bubble.
 *
 * @param role "user" or "assistant"
 * @param content the message text
 * @param isStreaming when true, appends a blinking cursor to the text (assistant only)
 * @param codeBlock optional pair of (language, code) rendered as a surfaceDarkest inset
 */
@Composable
fun ChatBubble(
    role: String,
    content: String,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    codeBlock: Pair<String, String>? = null,
) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    val isUser = role == "user"
    val bubbleColor = if (isUser) colors.blue else colors.surfaceDark
    val textColor = if (isUser) Color.White else colors.textOnDark

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.85f)
                .clip(
                    if (isUser) {
                        // bottom-right small (4dp) tail
                        RoundedCornerShape(
                            topStart = 12.dp, topEnd = 12.dp,
                            bottomStart = 12.dp, bottomEnd = 4.dp,
                        )
                    } else {
                        RoundedCornerShape(
                            topStart = 12.dp, topEnd = 12.dp,
                            bottomStart = 4.dp, bottomEnd = 12.dp,
                        )
                    }
                )
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (content.isNotEmpty()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    BasicText(
                        text = content,
                        style = typography.bodyLarge.copy(color = textColor),
                    )
                    if (isStreaming && !isUser) {
                        Spacer(Modifier.width(2.dp))
                        StreamingCursor()
                    }
                }
            } else if (isStreaming) {
                StreamingCursor()
            }
            if (codeBlock != null) {
                if (content.isNotEmpty()) Spacer(Modifier.height(8.dp))
                CodeBlock(language = codeBlock.first, code = codeBlock.second)
            }
        }
    }
}

@Composable
private fun CodeBlock(language: String, code: String) {
    val colors = LocalColors.current
    val shapes = LocalShapes.current
    val typography = LocalTypography.current

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shapes.codeBlock)
            .background(colors.surfaceDarkest)
            .border(0.5.dp, colors.outlineDark, shapes.codeBlock)
            .padding(12.dp)
            .clipToBounds(),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = language.uppercase(),
                style = typography.toolLabel.copy(color = colors.textOnDarkTertiary),
            )
            BasicText(
                text = "copy",
                style = typography.toolLabel.copy(color = colors.textOnDarkSecondary),
            )
        }
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = code,
            style = typography.codeBlock.copy(color = colors.codePlain),
            overflow = TextOverflow.Visible,
        )
    }
}
