package com.confused.onlylist.designsystem.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * FontRegistry — maps token keys (body / display / mono) → FontFamily.
 *
 * v1 uses FontFamily.Default / SansSerif / Monospace (system fonts).
 * Phase 1.5 will bundle Inter + Sora + JetBrains Mono as variable fonts in res/font/
 * and swap the defaults. Past bold-rendering issues came from missing weight files —
 * when bundling, include ALL weights (400, 500, 600, 700, 800).
 *
 * The Typography.kt references these keys; swapping here swaps everywhere.
 */
object FontRegistry {
    val body: FontFamily = FontFamily.Default        // ponytail: Inter variable (400/500/600)
    val display: FontFamily = FontFamily.SansSerif   // ponytail: Sora variable (600/700/800)
    val mono: FontFamily = FontFamily.Monospace       // ponytail: JetBrains Mono variable (400/500)
}
