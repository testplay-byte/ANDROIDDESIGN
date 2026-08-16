package com.confused.agenttech.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.confused.agenttech.database.entity.ToolCallEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolCallDao {

    @Upsert
    suspend fun upsert(call: ToolCallEntity)

    @Query("SELECT * FROM tool_call WHERE messageId = :messageId ORDER BY timestamp ASC")
    fun observeByMessage(messageId: String): Flow<List<ToolCallEntity>>

    @Query("SELECT * FROM tool_call WHERE messageId = :messageId ORDER BY timestamp ASC")
    suspend fun getByMessage(messageId: String): List<ToolCallEntity>

    @Query("DELETE FROM tool_call WHERE id = :id")
    suspend fun delete(id: String)
}
