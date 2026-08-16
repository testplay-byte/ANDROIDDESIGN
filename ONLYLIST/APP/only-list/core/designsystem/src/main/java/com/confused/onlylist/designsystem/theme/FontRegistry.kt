package com.confused.onlylist.designsystem.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.confused.onlylist.designsystem.R

/**
 * FontRegistry — maps token keys (body / display / mono) → FontFamily.
 *
 * Uses BUNDLED variable fonts (Inter, Sora, JetBrains Mono — all OFL).
 * Variable fonts cover ALL weights (400-800) from a single file — the system
 * renderer picks the right weight from the `wght` variation axis based on the
 * FontWeight requested in the TextStyle. This fixes the bold-rendering issues
 * the user experienced (missing weight files) — variable fonts always have
 * every weight available.
 *
 * Total APK cost: ~1.2MB for all three families.
 */
object FontRegistry {
    val body: FontFamily = FontFamily(Font(R.font.inter_variable))
    val display: FontFamily = FontFamily(Font(R.font.sora_variable))
    val mono: FontFamily = FontFamily(Font(R.font.jetbrains_mono_variable))
}
