package com.confused.agenttech.database

import android.content.Context
import androidx.room.Room

/**
 * Database provider — singleton access to the Room database.
 * Will be replaced by Koin DI in a later phase; for now it's a simple object.
 */
object DatabaseProvider {

    @Volatile
    private var instance: AgentTechDatabase? = null

    fun get(context: Context): AgentTechDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AgentTechDatabase::class.java,
                AgentTechDatabase.DATABASE_NAME,
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
