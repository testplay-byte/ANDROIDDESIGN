package com.confused.agenttech.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Typography — see APP/agent-tech/DESIGN-LANGUAGE.md §2 ──
//
// Uses BUNDLED variable fonts (Inter body, Sora display, JetBrains Mono code)
// via FontRegistry. Variable fonts cover all weights — fixes the
// bold-rendering issues that bare FontFamily(Font(...)) causes.

@Immutable
data class AgentTypography(
    val displayLarge: TextStyle = TextStyle(
        fontFamily = FontRegistry.display,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    val displayMedium: TextStyle = TextStyle(
        fontFamily = FontRegistry.display,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    val headingLarge: TextStyle = TextStyle(
        fontFamily = FontRegistry.display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    val titleLarge: TextStyle = TextStyle(
        fontFamily = FontRegistry.body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    val titleMedium: TextStyle = TextStyle(
        fontFamily = FontRegistry.body,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = FontRegistry.body,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = FontRegistry.body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    val bodySmall: TextStyle = TextStyle(
        fontFamily = FontRegistry.body,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = FontRegistry.body,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    val codeBlock: TextStyle = TextStyle(
        fontFamily = FontRegistry.mono,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    ),
    val codeInline: TextStyle = TextStyle(
        fontFamily = FontRegistry.mono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    val toolLabel: TextStyle = TextStyle(
        fontFamily = FontRegistry.mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
    val micro: TextStyle = TextStyle(
        fontFamily = FontRegistry.body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
)

@Stable
val DefaultTypography = AgentTypography()
