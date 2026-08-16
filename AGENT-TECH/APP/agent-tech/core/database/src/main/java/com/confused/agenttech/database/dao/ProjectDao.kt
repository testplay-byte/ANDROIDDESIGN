package com.confused.agenttech.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.confused.agenttech.database.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Upsert
    suspend fun upsert(project: ProjectEntity)

    @Query("SELECT * FROM project ORDER BY lastActiveAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM project WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Query("SELECT COUNT(*) FROM project")
    suspend fun count(): Int

    @Query("DELETE FROM project WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE project SET lastActiveAt = :timestamp WHERE id = :id")
    suspend fun touch(id: String, timestamp: Long)
}
