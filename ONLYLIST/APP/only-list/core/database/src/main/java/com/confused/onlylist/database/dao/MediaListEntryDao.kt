package com.confused.onlylist.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.confused.onlylist.database.entity.MediaListEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaListEntryDao {

    @Upsert
    suspend fun upsertAll(entries: List<MediaListEntryEntity>)

    @Upsert
    suspend fun upsert(entry: MediaListEntryEntity)

    @Query("SELECT * FROM media_list_entry ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<MediaListEntryEntity>>

    @Query("SELECT * FROM media_list_entry WHERE status = :status ORDER BY updatedAt DESC")
    fun getByStatus(status: String): Flow<List<MediaListEntryEntity>>

    @Query("SELECT * FROM media_list_entry WHERE mediaId = :mediaId")
    fun getByMediaId(mediaId: Int): Flow<MediaListEntryEntity?>

    @Query("SELECT COUNT(*) FROM media_list_entry")
    suspend fun count(): Int
}
