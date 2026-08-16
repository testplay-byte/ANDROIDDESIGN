package com.confused.onlylist.database

import android.content.Context
import androidx.room.Room
import com.confused.onlylist.database.OnlyListDatabase

/**
 * Database provider — singleton access to the Room database.
 * Phase 2 will replace this with Koin DI; for now it's a simple object.
 */
object DatabaseProvider {

    @Volatile
    private var instance: OnlyListDatabase? = null

    fun get(context: Context): OnlyListDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                OnlyListDatabase::class.java,
                OnlyListDatabase.DATABASE_NAME,
            )
                // Per CORE_RULES §25: debug builds can rebuild schema freely.
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
