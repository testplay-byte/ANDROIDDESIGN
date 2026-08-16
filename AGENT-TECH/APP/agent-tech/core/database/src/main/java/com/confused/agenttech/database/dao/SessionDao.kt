package com.confused.agenttech.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.confused.agenttech.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM session WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeByProject(projectId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?

    @Query("SELECT * FROM session WHERE id = :id")
    fun observeById(id: String): Flow<SessionEntity?>

    @Query("UPDATE session SET status = :status, updatedAt = :timestamp WHERE id = :id")
    suspend fun setStatus(id: String, status: String, timestamp: Long)

    @Query("UPDATE session SET inputTokens = :input, outputTokens = :output, costMicros = :cost, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateUsage(id: String, input: Long, output: Long, cost: Long, timestamp: Long)

    @Query("DELETE FROM session WHERE id = :id")
    suspend fun delete(id: String)
}
