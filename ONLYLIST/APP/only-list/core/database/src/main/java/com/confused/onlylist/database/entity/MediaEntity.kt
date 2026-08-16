package com.confused.onlylist.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Media entity — caches anime/manga metadata from AniList.
 * Per CORE_RULES §14 (offline-first): the UI reads from Room first, network refreshes.
 */
@Entity(
    tableName = "media",
    indices = [
        Index(value = ["idMal"]),
        Index(value = ["type", "status"]),
        Index(value = ["titleRomaji"]),
    ]
)
data class MediaEntity(
    @PrimaryKey
    val id: Int,                    // AniList media ID
    val idMal: Int? = null,        // MyAnimeList ID (for Kitsu/Jikan mapping)
    val type: String,               // ANIME or MANGA
    val titleRomaji: String? = null,
    val titleEnglish: String? = null,
    val titleNative: String? = null,
    val coverImageLarge: String? = null,
    val coverImageColor: String? = null,  // hex tint for placeholder
    val bannerImage: String? = null,
    val episodes: Int? = null,     // null for manga (chapters instead)
    val chapters: Int? = null,
    val duration: Int? = null,      // per-episode duration (min)
    val status: String? = null,     // FINISHED, RELEASING, NOT_YET_RELEASED, CANCELLED
    val season: String? = null,     // WINTER, SPRING, SUMMER, FALL
    val seasonYear: Int? = null,
    val format: String? = null,     // TV, MOVIE, OVA, ONA, SPECIAL, NOVEL
    val source: String? = null,
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val popularity: Int? = null,
    val favourites: Int? = null,
    val genres: String = "[]",       // JSON array: ["Action","Drama"]
    val description: String? = null,
    val nextAiringEpisode: Int? = null,
    val nextAiringAt: Long? = null,  // unix timestamp
    val updatedAt: Long = 0,         // AniList Media.updatedAt
    val lastFetchedAt: Long = 0,     // our fetch timestamp
)
