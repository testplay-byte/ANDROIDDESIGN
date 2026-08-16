package com.confused.agenttech.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single tool call invoked by the assistant.
 * status: queued / running / success / error / needs-approval / approved.
 * input / output are JSON strings (the tool's argument map and result object).
 */
@Entity(
    tableName = "tool_call",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["messageId"])],
)
data class ToolCallEntity(
    @PrimaryKey
    val id: String,
    val messageId: String,
    val toolName: String,
    val input: String,
    val output: String,
    val status: String,
    val timestamp: Long,
)
