package com.confused.agenttech.agent.tools

import com.confused.agenttech.common.Logger
import kotlinx.coroutines.CompletableDeferred

/**
 * AskUserTool — pauses the agent and asks the user a question.
 *
 * Returns a [ToolResult] once the user responds via the UI. The runtime
 * bridges the UI's response back into this tool via [respond].
 *
 * Input:
 *   - `question` (required): the question to show the user
 *
 * Output: the user's response text.
 */
class AskUserTool : Tool {

    override val name: String = "ask_user"
    override val description: String =
        "Ask the user a clarifying question and wait for their response. " +
        "Use when you lack information needed to proceed. " +
        "Input: {\"question\": \"Which version should I target?\"}."

    private var pending: CompletableDeferred<String>? = null

    /** Called by the UI layer when the user submits a response. */
    fun respond(answer: String) {
        pending?.complete(answer)
        pending = null
        Logger.d("Tool:AskUser", "user responded: $answer")
    }

    /** True if this tool is currently waiting for a user response. */
    val isAwaiting: Boolean get() = pending?.isActive == true

    override suspend fun execute(input: ToolInput): ToolResult {
        val question = input.require("question")
        val deferred = CompletableDeferred<String>()
        pending = deferred
        Logger.d("Tool:AskUser", "asking: $question")
        val answer = deferred.await() // suspends until respond() is called
        ToolResult(ToolResultStatus.SUCCESS, "User answered: $answer")
            .let { return it }
    }
}
