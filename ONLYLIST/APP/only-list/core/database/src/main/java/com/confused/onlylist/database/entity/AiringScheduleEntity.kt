package com.confused.onlylist.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Airing schedule — upcoming episodes from AniList AiringSchedule.
 */
@Entity(
    tableName = "airing_schedule",
    indices = [Index(value = ["mediaId", "episode"])]
)
data class AiringScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Int,
    val episode: Int,
    val airingAt: Long,              // unix timestamp
    val lastFetchedAt: Long = 0,
)
