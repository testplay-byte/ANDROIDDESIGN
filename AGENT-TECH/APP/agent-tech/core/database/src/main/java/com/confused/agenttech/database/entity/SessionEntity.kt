package com.confused.agenttech.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A session — a single agent run within a project.
 * status: active / idle / running / error / stopped / success.
 */
@Entity(
    tableName = "session",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["projectId"])],
)
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val title: String = "",
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val costMicros: Long = 0,   // in micros (1e-6) of the user's currency unit
)
