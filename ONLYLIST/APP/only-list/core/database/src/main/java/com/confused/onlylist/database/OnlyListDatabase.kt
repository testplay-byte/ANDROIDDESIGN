package com.confused.onlylist.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.confused.onlylist.database.dao.AiringScheduleDao
import com.confused.onlylist.database.dao.EpisodeDao
import com.confused.onlylist.database.dao.MediaDao
import com.confused.onlylist.database.dao.MediaListEntryDao
import com.confused.onlylist.database.dao.MetadataSourceStateDao
import com.confused.onlylist.database.entity.AiringScheduleEntity
import com.confused.onlylist.database.entity.EpisodeEntity
import com.confused.onlylist.database.entity.MediaEntity
import com.confused.onlylist.database.entity.MediaListEntryEntity
import com.confused.onlylist.database.entity.MetadataSourceStateEntity

/**
 * Only-List Room database.
 * Per CORE_RULES §25 (Debug-Build Schema Freedom): debug builds can rebuild
 * the schema freely — `fallbackToDestructiveMigration` is acceptable.
 */
@Database(
    entities = [
        MediaEntity::class,
        EpisodeEntity::class,
        MediaListEntryEntity::class,
        AiringScheduleEntity::class,
        MetadataSourceStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class OnlyListDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun mediaListEntryDao(): MediaListEntryDao
    abstract fun airingScheduleDao(): AiringScheduleDao
    abstract fun metadataSourceStateDao(): MetadataSourceStateDao

    companion object {
        const val DATABASE_NAME = "only-list.db"
    }
}
