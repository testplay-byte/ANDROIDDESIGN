package com.confused.onlylist.designsystem.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.confused.onlylist.designsystem.R

/**
 * FontRegistry — maps token keys (body / display / mono) → FontFamily.
 *
 * Uses BUNDLED variable fonts (Inter, Sora, JetBrains Mono — all OFL).
 *
 * CRITICAL FIX (R-9): each weight must be registered as a SEPARATE Font entry
 * with explicit `variationSettings = FontVariation.Settings(FontVariation.weight(N))`.
 * A bare `FontFamily(Font(R.font.inter_variable))` only registers the default
 * weight (400) and silently ignores any `FontWeight.Bold` in a TextStyle —
 * which is why bold headings were rendering as Regular.
 *
 * Reference:
 *   - developer.android.com/develop/ui/compose/text/fonts (variable font section)
 *   - "Just Your Type: Variable Fonts in Compose" — Chris Banes, Nov 2022
 *
 * Variable fonts require Android O (API 26). Our minSdk is API 26 — exact match.
 *
 * Total APK cost: ~1.2MB for all three families (one variable file each).
 */
@OptIn(ExperimentalTextApi::class)
object FontRegistry {

    /** Inter — body / UI text. Weights 400/500/600/700. */
    val body: FontFamily = FontFamily(
        Font(
            R.font.inter_variable,
            weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        ),
        Font(
            R.font.inter_variable,
            weight = FontWeight.Medium,
            variationSettings = FontVariation.Settings(FontVariation.weight(500)),
        ),
        Font(
            R.font.inter_variable,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(600)),
        ),
        Font(
            R.font.inter_variable,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700)),
        ),
    )

    /** Sora — display / headlines. Weights 600/700/800. */
    val display: FontFamily = FontFamily(
        Font(
            R.font.sora_variable,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(600)),
        ),
        Font(
            R.font.sora_variable,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700)),
        ),
        Font(
            R.font.sora_variable,
            weight = FontWeight.ExtraBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(800)),
        ),
    )

    /** JetBrains Mono — numbers / tabular data. Weights 400/500/600. */
    val mono: FontFamily = FontFamily(
        Font(
            R.font.jetbrains_mono_variable,
            weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        ),
        Font(
            R.font.jetbrains_mono_variable,
            weight = FontWeight.Medium,
            variationSettings = FontVariation.Settings(FontVariation.weight(500)),
        ),
        Font(
            R.font.jetbrains_mono_variable,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(600)),
        ),
    )
}
