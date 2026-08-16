package com.confused.agenttech.agent.core

import com.confused.agenttech.agent.llm.ChatMessage
import com.confused.agenttech.agent.llm.LlmProvider
import com.confused.agenttech.agent.tools.ToolInput
import com.confused.agenttech.agent.tools.ToolRegistry
import com.confused.agenttech.agent.tools.ToolResult
import com.confused.agenttech.agent.tools.ToolResultStatus
import com.confused.agenttech.common.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * AgentRuntime — the iterative agent loop.
 *
 * Each iteration:
 *   1. Build context (system + conversation) via [ContextManager].
 *   2. Stream an LLM completion.
 *   3. Parse any tool calls out of the assistant message.
 *   4. Execute each tool via [ToolRegistry], appending the result to context.
 *   5. Repeat until: an `attempt_completion` tool runs, the iteration cap is
 *      reached, or the user cancels.
 *
 * Abortable: the running loop can be cancelled via [stop] (coroutine cancel).
 *
 * Tool-call parsing in v1 is intentionally simple: the assistant message
 * may contain a JSON block of shape
 *   ```json
 *   { "tool": "read_file", "arguments": { "path": "..." } }
 *   ```
 * at the end of the message. v2 will switch to OpenAI function-calling.
 */
class AgentRuntime(
    private val provider: LlmProvider,
    private val toolRegistry: ToolRegistry,
    private val contextManager: ContextManager = ContextManager(),
    private val maxIterations: Int = 25,
) {

    private val _events = MutableSharedFlow<AgentEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<AgentEvent> = _events.asSharedFlow()

    private var runJob: Job? = null

    /** True when the loop is currently running. */
    val isRunning: Boolean get() = runJob?.isActive == true

    /**
     * Start a run. The user message is appended to [history] (already persisted
     * by the caller) before this is invoked.
     */
    fun run(
        scope: CoroutineScope,
        history: List<ChatMessage>,
        onAssistantToken: (String) -> Unit,
        onAssistantMessage: (String) -> Unit,
    ): Job {
        if (runJob?.isActive == true) return runJob!!
        runJob = scope.launch(Dispatchers.Default) {
            runLoop(history, onAssistantToken, onAssistantMessage)
        }
        return runJob!!
    }

    /** Cancel the current run (if any). Safe to call from the UI thread. */
    fun stop() {
        runJob?.cancel()
        runJob = null
    }

    private suspend fun runLoop(
        history: List<ChatMessage>,
        onAssistantToken: (String) -> Unit,
        onAssistantMessage: (String) -> Unit,
    ) {
        var conversation = history
        var iteration = 0
        Logger.i("Agent", "▶ run start (maxIter=$maxIterations)")

        try {
            while (isActive && iteration < maxIterations) {
                iteration++
                Logger.d("Agent", "iteration $iteration / $maxIterations")

                val truncated = contextManager.truncate(conversation)
                _events.tryEmit(AgentEvent.IterationStarted(iteration))

                val assistantText = provider.stream(truncated) { tok ->
                    onAssistantToken(tok)
                    _events.tryEmit(AgentEvent.Token(tok))
                }.getOrElse { e ->
                    Logger.e("Agent", "LLM stream failed: ${e.message}", e)
                    _events.tryEmit(AgentEvent.Error(e.message ?: "Unknown error"))
                    return
                }

                onAssistantMessage(assistantText)
                conversation = conversation + ChatMessage("assistant", assistantText)

                val toolCall = parseToolCall(assistantText)
                if (toolCall == null) {
                    // No tool call — assistant gave a final answer.
                    Logger.i("Agent", "✓ no tool call → done")
                    _events.tryEmit(AgentEvent.Completed(assistantText))
                    return
                }

                _events.tryEmit(AgentEvent.ToolCallStarted(toolCall.first, iteration))
                val result: ToolResult = toolRegistry.execute(toolCall.first, toolCall.second)
                _events.tryEmit(
                    AgentEvent.ToolCallFinished(toolCall.first, result.status, result.output)
                )

                // attempt_completion → stop.
                if (toolCall.first == "attempt_completion") {
                    Logger.i("Agent", "✓ attempt_completion → done")
                    _events.tryEmit(AgentEvent.Completed(assistantText))
                    return
                }

                // Append tool result to context (as a "tool" role message).
                conversation = conversation + ChatMessage("tool", result.output)
            }
            if (iteration >= maxIterations) {
                Logger.w("Agent", "✗ maxIterations ($maxIterations) reached")
                _events.tryEmit(AgentEvent.MaxIterationsReached(maxIterations))
            }
        } finally {
            Logger.i("Agent", "■ run end (iterations=$iteration)")
        }
    }

    /**
     * Parse a trailing JSON tool-call block out of an assistant message.
     * Returns null if no tool call was found.
     */
    private fun parseToolCall(text: String): Pair<String, ToolInput>? {
        val jsonBlock = findJsonBlock(text) ?: return null
        return try {
            val obj = Json.parseToJsonElement(jsonBlock) as JsonObject
            val tool = (obj["tool"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?: return null
            val argsObj = obj["arguments"] as? JsonObject ?: JsonObject(emptyMap())
            val args = argsObj.mapValues { entry ->
                (entry.value as? kotlinx.serialization.json.JsonPrimitive)?.content
                    ?: entry.value.toString()
            }
            tool to ToolInput(args)
        } catch (e: Exception) {
            Logger.w("Agent", "tool call parse failed: ${e.message}")
            null
        }
    }

    /** Finds a ```json ... ``` block or a final {...} block at the end of text. */
    private fun findJsonBlock(text: String): String? {
        val fence = text.indexOf("```json")
        if (fence >= 0) {
            val start = fence + "```json".length
            val end = text.indexOf("```", start)
            if (end > start) return text.substring(start, end).trim()
        }
        val lastBrace = text.lastIndexOf('{')
        val lastClose = text.lastIndexOf('}')
        if (lastBrace >= 0 && lastClose > lastBrace) {
            return text.substring(lastBrace, lastClose + 1).trim()
        }
        return null
    }
}

/** Sealed event hierarchy surfaced to the UI via [AgentRuntime.events]. */
sealed class AgentEvent {
    data class IterationStarted(val iteration: Int) : AgentEvent()
    data class Token(val token: String) : AgentEvent()
    data class ToolCallStarted(val tool: String, val iteration: Int) : AgentEvent()
    data class ToolCallFinished(
        val tool: String,
        val status: ToolResultStatus,
        val output: String,
    ) : AgentEvent()
    data class Completed(val finalText: String) : AgentEvent()
    data class MaxIterationsReached(val max: Int) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
}
