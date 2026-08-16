package com.confused.agenttech.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A project — a SAF-scoped folder the agent operates within.
 * `folderUri` is the SAF tree URI (content://com.android.externalstorage.documents/tree/...).
 */
@Entity(
    tableName = "project",
    indices = [Index(value = ["folderUri"], unique = true)],
)
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val folderUri: String,
    val createdAt: Long,
    val lastActiveAt: Long,
)
