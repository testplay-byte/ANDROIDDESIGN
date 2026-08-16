package com.confused.onlylist.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-source fetch state — for reconciliation + backoff.
 * Per CORE_RULES §15 rule 7: on 404/empty, increment failureCount + set backoffUntil.
 */
@Entity(tableName = "metadata_source_state")
data class MetadataSourceStateEntity(
    @PrimaryKey
    val key: String,                // "{mediaId}_{source}" e.g. "123_anilist"
    val mediaId: Int,
    val source: String,             // ANILIST, KITSU, JIKAN
    val lastFetchedAt: Long = 0,
    val lastSuccessAt: Long = 0,
    val failureCount: Int = 0,
    val backoffUntil: Long = 0,      // unix timestamp; 0 = no backoff
)
