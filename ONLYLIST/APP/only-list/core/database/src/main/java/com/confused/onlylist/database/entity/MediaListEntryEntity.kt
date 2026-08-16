package com.confused.onlylist.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User's list entry — from AniList MediaListCollection.
 */
@Entity(
    tableName = "media_list_entry",
    indices = [Index(value = ["mediaId"], unique = true)]
)
data class MediaListEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Int,
    val status: String,              // CURRENT, COMPLETED, PAUSED, DROPPED, PLANNING, REPEATING
    val score: Double = 0.0,
    val progress: Int = 0,           // episodes watched / chapters read
    val progressVolumes: Int = 0,    // manga volumes read
    val repeat: Int = 0,
    val notes: String? = null,
    val priority: Int = 0,
    val private: Boolean = false,
    val startedAt: String? = null,   // ISO date
    val completedAt: String? = null,  // ISO date
    val updatedAt: Long = 0,          // AniList MediaList.updatedAt
    val lastFetchedAt: Long = 0,
)
