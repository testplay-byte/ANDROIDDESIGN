package com.confused.agenttech.agent.tools

/**
 * AttemptCompletionTool — signals that the agent considers its task done.
 *
 * The runtime observes this and stops the loop.
 *
 * Input:
 *   - `summary` (required): a short final summary of what was done.
 */
class AttemptCompletionTool : Tool {

    override val name: String = "attempt_completion"
    override val description: String =
        "Signal that the task is complete. Use when you've accomplished the user's " +
        "request and have nothing more to do. " +
        "Input: {\"summary\": \"Created Foo.kt and added unit test.\"}."

    override suspend fun execute(input: ToolInput): ToolResult {
        val summary = input.require("summary")
        return ToolResult(ToolResultStatus.SUCCESS, "TASK_COMPLETE: $summary")
    }
}
