package com.confused.onlylist.ui.components

import androidx.compose.ui.graphics.Color
import com.confused.onlylist.data.mock.MockMedia
import com.confused.onlylist.data.mock.MediaStatus
import com.confused.onlylist.database.entity.MediaEntity

/**
 * Maps a Room MediaEntity to the UI model (MockMedia — will be renamed MediaUiModel in Phase 3).
 * Per CORE_RULES §14: UI reads from Room; this mapping bridges the domain entity → UI model.
 */
fun MediaEntity.toUiModel(): MockMedia = MockMedia(
    id = id,
    title = titleRomaji ?: titleEnglish ?: titleNative ?: "Unknown",
    titleEnglish = titleEnglish ?: title,
    coverColor = parseCoverColor(coverImageColor),
    score = (averageScore ?: 0) / 10.0,
    episodes = episodes ?: 0,
    progress = 0,
    status = if (status == "RELEASING") MediaStatus.AIRING else MediaStatus.COMPLETED,
    format = format ?: "TV",
    season = season ?: "",
    year = seasonYear ?: 0,
    genres = emptyList(),
    description = description ?: "",
    nextEpisode = nextAiringEpisode,
    nextAiringAt = nextAiringAt?.let { "soon" },
)

private fun parseCoverColor(hex: String?): Color {
    if (hex == null) return Color(0xFF4A3B6B)
    return try {
        val cleaned = hex.removePrefix("#")
        val long = if (cleaned.length == 6) "FF$cleaned".toLong(16) else cleaned.toLong(16)
        Color(long.toInt())
    } catch (e: Exception) {
        Color(0xFF4A3B6B)
    }
}
