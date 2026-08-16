package com.confused.agenttech.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.confused.agenttech.database.entity.ProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {

    @Upsert
    suspend fun upsert(provider: ProviderEntity)

    @Query("SELECT * FROM provider ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM provider ORDER BY createdAt ASC")
    suspend fun getAll(): List<ProviderEntity>

    @Query("SELECT * FROM provider WHERE id = :id")
    suspend fun getById(id: String): ProviderEntity?

    @Query("SELECT * FROM provider WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<ProviderEntity?>

    @Query("SELECT * FROM provider WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): ProviderEntity?

    @Query("UPDATE provider SET isActive = (id = :id)")
    suspend fun setActive(id: String)

    @Query("DELETE FROM provider WHERE id = :id")
    suspend fun delete(id: String)
}
