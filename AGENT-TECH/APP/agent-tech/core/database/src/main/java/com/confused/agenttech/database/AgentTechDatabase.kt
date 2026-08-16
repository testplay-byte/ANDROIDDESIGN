package com.confused.agenttech.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.confused.agenttech.database.dao.MessageDao
import com.confused.agenttech.database.dao.ProjectDao
import com.confused.agenttech.database.dao.ProviderDao
import com.confused.agenttech.database.dao.SessionDao
import com.confused.agenttech.database.dao.ToolCallDao
import com.confused.agenttech.database.dao.UsageLogDao
import com.confused.agenttech.database.entity.MessageEntity
import com.confused.agenttech.database.entity.ProjectEntity
import com.confused.agenttech.database.entity.ProviderEntity
import com.confused.agenttech.database.entity.SessionEntity
import com.confused.agenttech.database.entity.ToolCallEntity
import com.confused.agenttech.database.entity.UsageLogEntity

/**
 * Agent Tech Room database.
 *
 * Version 1. `fallbackToDestructiveMigration` is acceptable during the v0.x
 * prototyping phase (per Only-List precedent + the user's "speed > rigor for
 * the schema" preference).
 */
@Database(
    entities = [
        ProjectEntity::class,
        SessionEntity::class,
        MessageEntity::class,
        ToolCallEntity::class,
        ProviderEntity::class,
        UsageLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AgentTechDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun toolCallDao(): ToolCallDao
    abstract fun providerDao(): ProviderDao
    abstract fun usageLogDao(): UsageLogDao

    companion object {
        const val DATABASE_NAME = "agent-tech.db"
    }
}
