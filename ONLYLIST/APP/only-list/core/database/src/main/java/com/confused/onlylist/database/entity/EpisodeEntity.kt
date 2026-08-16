package com.confused.onlylist.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Episode metadata — merged from Kitsu (primary) + Jikan (filler/dates) + AniList (count/next).
 * Per CORE_RULES §15: multi-source merge, append-never-overwrite.
 */
@Entity(
    tableName = "episode",
    indices = [
        Index(value = ["mediaId", "episodeNumber"], unique = true),
    ]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Int,                // AniList media ID
    val episodeNumber: Int,
    val titleEn: String? = null,     // from Kitsu canonicalTitle / Jikan title
    val titleJp: String? = null,     // from Kitsu titles.ja_jp / Jikan title_japanese
    val synopsis: String? = null,    // from Kitsu synopsis / Jikan synopsis (lazy)
    val airDate: String? = null,     // ISO8601 from Jikan aired / Kitsu airdate
    val thumbnailUrl: String? = null,// from Kitsu thumbnail.original
    val durationMinutes: Int? = null, // from Kitsu length / Jikan duration (sec→min)
    val filler: Boolean = false,     // from Jikan filler flag
    val recap: Boolean = false,     // from Jikan recap flag
    val lastFetchedAt: Long = 0,
)
