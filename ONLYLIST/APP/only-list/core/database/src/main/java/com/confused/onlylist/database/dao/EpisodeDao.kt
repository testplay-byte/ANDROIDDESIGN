package com.confused.onlylist.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.confused.onlylist.database.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Upsert
    suspend fun upsertAll(episodes: List<EpisodeEntity>)

    @Upsert
    suspend fun upsert(episode: EpisodeEntity)

    @Query("SELECT * FROM episode WHERE mediaId = :mediaId ORDER BY episodeNumber ASC")
    fun getByMediaId(mediaId: Int): Flow<List<EpisodeEntity>>

    @Query("SELECT COUNT(*) FROM episode WHERE mediaId = :mediaId")
    suspend fun countByMediaId(mediaId: Int): Int
}
