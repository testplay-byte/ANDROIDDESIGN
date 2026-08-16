package com.confused.agenttech.agent.llm

/**
 * LlmProvider — the streaming interface implemented by every backend
 * (OpenAI / Anthropic / Ollama / LM Studio / OpenRouter / etc.).
 *
 * `stream` consumes a list of [ChatMessage] and emits each incremental token
 * via [onToken]. Returns the final assembled string on success or an
 * exception on failure.
 */
interface LlmProvider {

    /** Display name (e.g. "OpenAI / gpt-4o"). */
    val displayName: String

    /**
     * Stream a completion. The implementation MUST support cooperative
     * cancellation — if the calling coroutine is cancelled, the network
     * request is aborted and no further tokens are emitted.
     */
    suspend fun stream(
        messages: List<ChatMessage>,
        onToken: (String) -> Unit,
    ): Result<String>

    /**
     * Optional: a one-shot non-streaming call used by "Test Connection"
     * in the provider config screen.
     */
    suspend fun testConnection(): Result<String> =
        stream(listOf(ChatMessage("user", "ping")), onToken = {})
}
