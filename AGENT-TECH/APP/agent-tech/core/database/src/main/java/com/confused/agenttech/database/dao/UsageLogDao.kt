package com.confused.agenttech.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.confused.agenttech.database.entity.UsageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLogDao {

    @Upsert
    suspend fun upsert(log: UsageLogEntity)

    @Query("SELECT * FROM usage_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<UsageLogEntity>>

    @Query("SELECT * FROM usage_log WHERE providerId = :providerId ORDER BY timestamp DESC")
    fun observeByProvider(providerId: String): Flow<List<UsageLogEntity>>

    @Query("SELECT * FROM usage_log WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeBySession(sessionId: String): Flow<List<UsageLogEntity>>

    @Query("SELECT COALESCE(SUM(inputTokens), 0) FROM usage_log")
    fun observeTotalInputTokens(): Flow<Long>

    @Query("SELECT COALESCE(SUM(outputTokens), 0) FROM usage_log")
    fun observeTotalOutputTokens(): Flow<Long>

    @Query("SELECT COALESCE(SUM(costMicros), 0) FROM usage_log")
    fun observeTotalCostMicros(): Flow<Long>

    @Query("SELECT COUNT(*) FROM usage_log")
    fun observeRunCount(): Flow<Int>

    @Query("DELETE FROM usage_log WHERE id = :id")
    suspend fun delete(id: String)
}
