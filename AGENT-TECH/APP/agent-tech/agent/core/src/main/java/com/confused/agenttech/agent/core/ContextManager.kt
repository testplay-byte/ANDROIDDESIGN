package com.confused.agenttech.agent.core

import com.confused.agenttech.agent.llm.ChatMessage

/**
 * ContextManager — simple context management.
 *
 * Keeps the last N messages; truncates oldest when the limit is exceeded.
 *
 * v1 policy:
 *   - Keep the system prompt (first message) + the last (maxMessages - 1) messages.
 *   - This is the simplest "no token-counting" approach; works fine for short
 *     sessions. v2 will count tokens via tiktoken or the provider's count endpoint
 *     and LLM-based compaction for long sessions.
 */
class ContextManager(
    private val maxMessages: Int = 30,
) {

    fun truncate(messages: List<ChatMessage>): List<ChatMessage> {
        if (messages.size <= maxMessages) return messages
        val system = messages.firstOrNull { it.role == "system" }
        val rest = messages.filter { it.role != "system" }.takeLast(maxMessages - 1)
        return if (system != null) listOf(system) + rest else rest
    }
}
