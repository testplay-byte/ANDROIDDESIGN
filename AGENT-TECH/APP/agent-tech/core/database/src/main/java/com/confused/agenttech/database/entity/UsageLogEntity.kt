package com.confused.agenttech.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A usage log entry — one per LLM request, used for cost / token tracking
 * on the Usage screen.
 */
@Entity(
    tableName = "usage_log",
    foreignKeys = [
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["providerId"]), Index(value = ["sessionId"])],
)
data class UsageLogEntity(
    @PrimaryKey
    val id: String,
    val providerId: String,
    val sessionId: String,
    val inputTokens: Long,
    val outputTokens: Long,
    val costMicros: Long,
    val timestamp: Long,
)
