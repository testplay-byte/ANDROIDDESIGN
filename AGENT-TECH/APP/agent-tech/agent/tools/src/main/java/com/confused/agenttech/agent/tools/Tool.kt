package com.confused.agenttech.agent.tools

/**
 * Tool — the contract every agent-callable capability implements.
 *
 * Tools are scoped to a project (the SAF folder the user picked) and may
 * hold a [context] (android.content.Context) for ContentResolver access.
 */
interface Tool {
    val name: String
    val description: String

    suspend fun execute(input: ToolInput): ToolResult
}
