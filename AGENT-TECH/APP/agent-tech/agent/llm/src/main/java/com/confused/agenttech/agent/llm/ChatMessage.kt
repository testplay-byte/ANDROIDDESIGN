package com.confused.agenttech.agent.llm

import kotlinx.serialization.Serializable

/**
 * A single chat message — the OpenAI-style role/content shape.
 * `role` is "system" / "user" / "assistant" / "tool".
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)
