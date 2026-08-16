package com.confused.onlylist.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Typography — see APP/only-list/DESIGN-LANGUAGE.md §2 ──
//
// TODO(bundled-fonts): Phase 1 uses FontFamily.Default / SansSerif / Monospace as
// placeholders. Phase 1.5 will bundle Inter (body) + Sora (display) + JetBrains Mono
// (numbers) as variable fonts in res/font/ and swap FontFamily.Default → R.font.*.
// The FontRegistry is structured so this is a one-file change.
// Past bold-rendering issues came from missing weight files — bundle ALL weights.

@Immutable
data class OnlyListTypography(
    val displayLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,  // ponytail: Sora 700
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    val displayMedium: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,  // ponytail: Sora 700
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    val headingLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,  // ponytail: Sora 600
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    val titleLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,  // ponytail: Inter 600
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    val titleMedium: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,  // ponytail: Inter 500
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    val bodyLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,  // ponytail: Inter 400
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,  // ponytail: Inter 400
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    val bodySmall: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,  // ponytail: Inter 400
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,  // ponytail: Inter 500
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    val numberMedium: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,  // ponytail: JetBrains Mono 500
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    val numberLarge: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,  // ponytail: JetBrains Mono 600
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
)

@Stable
val DefaultTypography = OnlyListTypography()
