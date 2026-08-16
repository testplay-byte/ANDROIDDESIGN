package com.confused.onlylist.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Typography — see APP/only-list/DESIGN-LANGUAGE.md §2 ──
//
// Uses BUNDLED variable fonts (Inter for body, Sora for display, JetBrains Mono
// for numbers) via FontRegistry. Variable fonts cover all weights — fixes
// the bold-rendering issues the user experienced with missing weight files.

@Immutable
data class OnlyListTypography(
    val displayLarge: TextStyle = TextStyle(
        fontFamily = FontRegistry.display,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    val displayMedium: TextStyle = TextStyle(
        fontFamily = FontRegistry.display,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    val headingLarge: TextStyle = TextStyle(
        fontFamily = FontRegistry.display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    val titleLarge: TextStyle = TextStyle(
        fontFamily = FontRegistry.body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    val titleMedium: TextStyle = TextStyle(
        fontFamily = FontRegistry.body,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
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
    val numberMedium: TextStyle = TextStyle(
        fontFamily = FontRegistry.mono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    val numberLarge: TextStyle = TextStyle(
        fontFamily = FontRegistry.mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
)

@Stable
val DefaultTypography = OnlyListTypography()
