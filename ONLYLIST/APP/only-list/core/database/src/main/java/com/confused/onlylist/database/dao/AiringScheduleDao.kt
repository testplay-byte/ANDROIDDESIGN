package com.confused.onlylist.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.confused.onlylist.database.entity.AiringScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiringScheduleDao {

    @Upsert
    suspend fun upsertAll(schedule: List<AiringScheduleEntity>)

    @Query("SELECT * FROM airing_schedule WHERE airingAt >= :from ORDER BY airingAt ASC LIMIT :limit")
    fun getUpcoming(from: Long, limit: Int = 50): Flow<List<AiringScheduleEntity>>
}
