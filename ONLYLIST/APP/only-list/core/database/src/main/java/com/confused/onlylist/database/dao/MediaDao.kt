package com.confused.onlylist.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.confused.onlylist.database.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Upsert
    suspend fun upsertAll(media: List<MediaEntity>)

    @Upsert
    suspend fun upsert(media: MediaEntity)

    @Query("SELECT * FROM media WHERE id = :id")
    fun getById(id: Int): Flow<MediaEntity?>

    @Query("SELECT * FROM media WHERE type = :type ORDER BY popularity DESC LIMIT :limit")
    fun getTrending(type: String = "ANIME", limit: Int = 20): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE type = :type AND status = 'RELEASING' ORDER BY popularity DESC LIMIT :limit")
    fun getAiring(type: String = "ANIME", limit: Int = 20): Flow<List<MediaEntity>>

    @Query("""
        SELECT * FROM media
        WHERE type = :type
        AND (titleRomaji LIKE '%' || :query || '%'
          OR titleEnglish LIKE '%' || :query || '%'
          OR titleNative LIKE '%' || :query || '%')
        ORDER BY popularity DESC
        LIMIT :limit
    """)
    fun search(query: String, type: String = "ANIME", limit: Int = 50): Flow<List<MediaEntity>>

    @Query("SELECT COUNT(*) FROM media")
    suspend fun count(): Int
}
