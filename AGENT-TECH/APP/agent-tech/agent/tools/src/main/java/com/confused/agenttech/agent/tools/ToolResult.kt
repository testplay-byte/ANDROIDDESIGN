package com.confused.agenttech.agent.tools

import kotlinx.serialization.Serializable

/**
 * Result of a tool execution. The agent runtime observes the [status] +
 * [output] to decide the next iteration.
 */
@Serializable
data class ToolResult(
    val status: ToolResultStatus,
    val output: String,
    val isError: Boolean = status == ToolResultStatus.ERROR,
)

enum class ToolResultStatus { SUCCESS, ERROR, NEEDS_APPROVAL }
