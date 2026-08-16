package com.confused.onlylist.data.mock

import androidx.compose.ui.graphics.Color

/**
 * Mock data for Phase 1 placeholder screens.
 * Phase 2 will replace this with real AniList data via Room + repositories.
 */

data class MockMedia(
    val id: Int,
    val title: String,
    val titleEnglish: String,
    val coverColor: Color,
    val coverImageUrl: String? = null,
    val score: Double,
    val episodes: Int,
    val progress: Int,
    val status: MediaStatus,
    val format: String,
    val season: String,
    val year: Int,
    val genres: List<String>,
    val description: String,
    val nextEpisode: Int? = null,
    val nextAiringAt: String? = null,
)

enum class MediaStatus { CURRENT, COMPLETED, PAUSED, PLANNING, DROPPED, REPEATING, AIRING }

object MockData {

    val trending = listOf(
        MockMedia(
            id = 1,
            title = "Frieren: Beyond Journey's End",
            titleEnglish = "Frieren: Beyond Journey's End",
            coverColor = Color(0xFF6B8E9E),
            coverImageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-VdFOtersOvyy.jpg",
            score = 9.3,
            episodes = 28,
            progress = 28,
            status = MediaStatus.COMPLETED,
            format = "TV",
            season = "Fall",
            year = 2023,
            genres = listOf("Adventure", "Drama", "Fantasy"),
            description = "The mage Frieren continues her journey, reflecting on the time she spent with her former companions.",
        ),
        MockMedia(
            id = 2,
            title = "Solo Leveling",
            titleEnglish = "Solo Leveling",
            coverColor = Color(0xFF4A3B6B),
            coverImageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx145064-UEkRznSRn9F2.jpg",
            score = 8.5,
            episodes = 12,
            progress = 12,
            status = MediaStatus.COMPLETED,
            format = "TV",
            season = "Winter",
            year = 2024,
            genres = listOf("Action", "Adventure", "Fantasy"),
            description = "Sung Jin-Woo, the weakest hunter, gains the ability to level up infinitely.",
            nextEpisode = 13,
            nextAiringAt = "in 3 days",
        ),
        MockMedia(
            id = 3,
            title = "Dandadan",
            titleEnglish = "Dandadan",
            coverColor = Color(0xFFE55648),
            coverImageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx172718-fOtVzHQ6UG3H.jpg",
            score = 8.7,
            episodes = 12,
            progress = 8,
            status = MediaStatus.CURRENT,
            format = "TV",
            season = "Fall",
            year = 2024,
            genres = listOf("Action", "Comedy", "Supernatural"),
            description = "A skeptic boy and a ghost-believing girl investigate paranormal activity.",
            nextEpisode = 9,
            nextAiringAt = "in 1 day",
        ),
        MockMedia(
            id = 4,
            title = "Spy x Family Code: White",
            titleEnglish = "Spy x Family Code: White",
            coverColor = Color(0xFFD4A574),
            coverImageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx157652-CeDfYmJgH9xR.jpg",
            score = 8.2,
            episodes = 1,
            progress = 0,
            status = MediaStatus.PLANNING,
            format = "Movie",
            season = "Winter",
            year = 2024,
            genres = listOf("Action", "Comedy", "Slice of Life"),
            description = "The Forger family goes on a mission that could change the world.",
        ),
    )

    val currentlyWatching = listOf(
        MockData.trending[2],  // Dandadan
        MockMedia(
            id = 5,
            title = "One Piece",
            titleEnglish = "One Piece",
            coverColor = Color(0xFFE6B800),
            score = 8.9,
            episodes = 1100,
            progress = 1089,
            status = MediaStatus.CURRENT,
            format = "TV",
            season = "Fall",
            year = 1999,
            genres = listOf("Action", "Adventure", "Comedy"),
            description = "Monkey D. Luffy sets out to become the King of the Pirates.",
            nextEpisode = 1090,
            nextAiringAt = "in 2 days",
        ),
        MockMedia(
            id = 6,
            title = "Jujutsu Kaisen",
            titleEnglish = "Jujutsu Kaisen",
            coverColor = Color(0xFF2D5F4F),
            score = 8.6,
            episodes = 47,
            progress = 42,
            status = MediaStatus.CURRENT,
            format = "TV",
            season = "Fall",
            year = 2023,
            genres = listOf("Action", "Supernatural", "Horror"),
            description = "Yuji Itadori becomes the host of a powerful curse.",
            nextEpisode = 43,
            nextAiringAt = "in 4 days",
        ),
    )

    val completed = listOf(
        MockData.trending[0],  // Frieren
        MockData.trending[1],  // Solo Leveling
        MockMedia(
            id = 7,
            title = "Attack on Titan: Final Season",
            titleEnglish = "Attack on Titan: Final Season",
            coverColor = Color(0xFF8B5E3C),
            score = 9.1,
            episodes = 28,
            progress = 28,
            status = MediaStatus.COMPLETED,
            format = "TV",
            season = "Spring",
            year = 2023,
            genres = listOf("Action", "Drama", "Fantasy"),
            description = "The final battle for humanity's survival.",
        ),
        MockMedia(
            id = 8,
            title = "Demon Slayer: Infinity Castle",
            titleEnglish = "Demon Slayer: Infinity Castle",
            coverColor = Color(0xFFC44536),
            score = 8.8,
            episodes = 26,
            progress = 26,
            status = MediaStatus.COMPLETED,
            format = "TV",
            season = "Summer",
            year = 2024,
            genres = listOf("Action", "Supernatural"),
            description = "Tanjiro and the Hashira enter the Infinity Castle.",
        ),
    )

    val airingToday = listOf(
        MockData.trending[2],  // Dandadan — ep 9 in 1 day
        MockData.currentlyWatching[2],  // JJK — ep 43 in 4 days
    )

    val airingThisWeek = listOf(
        MockData.trending[1],  // Solo Leveling — ep 13 in 3 days
        MockData.currentlyWatching[1],  // One Piece — ep 1090 in 2 days
        MockData.trending[2],  // Dandadan — ep 9 in 1 day
        MockData.currentlyWatching[2],  // JJK — ep 43 in 4 days
    )
}
