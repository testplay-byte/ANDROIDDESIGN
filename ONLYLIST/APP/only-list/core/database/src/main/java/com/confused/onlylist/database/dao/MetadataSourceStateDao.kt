package com.confused.onlylist.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.confused.onlylist.database.entity.MetadataSourceStateEntity

@Dao
interface MetadataSourceStateDao {

    @Upsert
    suspend fun upsert(state: MetadataSourceStateEntity)

    @Query("SELECT * FROM metadata_source_state WHERE mediaId = :mediaId AND source = :source")
    suspend fun get(mediaId: Int, source: String): MetadataSourceStateEntity?
}
