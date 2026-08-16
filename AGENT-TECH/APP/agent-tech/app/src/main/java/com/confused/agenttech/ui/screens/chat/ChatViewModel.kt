package com.confused.agenttech.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.confused.agenttech.AppContainer
import com.confused.agenttech.agent.core.AgentEvent
import com.confused.agenttech.agent.core.AgentRuntime
import com.confused.agenttech.agent.llm.ChatMessage
import com.confused.agenttech.agent.tools.ToolResultStatus
import com.confused.agenttech.common.Logger
import com.confused.agenttech.database.entity.MessageEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ChatViewModel — manages the chat thread for the currently active project.
 *
 * On first send: creates a new session, persists the user message, builds
 * the [AgentRuntime], and streams the assistant response token-by-token into
 * the UI. Subsequent tool calls + iterations are appended to the message list.
 */
class ChatViewModel : ViewModel() {

    private val sessionRepo = AppContainer.sessionRepository
    private val projectRepo = AppContainer.projectRepository

    private var activeSessionId: String? = null
    private var runtime: AgentRuntime? = null
    private var runJob: Job? = null

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    val projects = projectRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    fun send() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _isStreaming.value) return
        _inputText.value = ""

        viewModelScope.launch {
            // Ensure we have an active session.
            val sessionId = ensureSession() ?: run {
                Logger.w("ChatVM", "no active project — abort send")
                return@launch
            }

            // Persist user message.
            val userMsg = sessionRepo.appendMessage(sessionId, "user", text)
            _messages.value = _messages.value + userMsg

            // Build runtime if needed.
            val rt = runtime ?: AppContainer.buildRuntime()?.also { runtime = it } ?: run {
                Logger.w("ChatVM", "no provider configured — abort send")
                // Surface an assistant error message inline.
                val err = sessionRepo.appendMessage(
                    sessionId, "assistant",
                    "⚠ No LLM provider configured. Open Settings → Provider Config to add one.",
                )
                _messages.value = _messages.value + err
                return@launch
            }

            _isStreaming.value = true
            _streamingText.value = ""

            // Subscribe to events.
            val eventJob = launch {
                rt.events.collect { event ->
                    when (event) {
                        is AgentEvent.Token -> {
                            _streamingText.value = _streamingText.value + event.token
                        }
                        is AgentEvent.ToolCallStarted -> {
                            // Drop a tool-call marker into the stream view.
                            _streamingText.value = _streamingText.value +
                                "\n\n[tool: ${event.tool}…]"
                        }
                        is AgentEvent.ToolCallFinished -> {
                            val statusLabel = when (event.status) {
                                ToolResultStatus.SUCCESS -> "✓"
                                ToolResultStatus.ERROR -> "✗"
                                ToolResultStatus.NEEDS_APPROVAL -> "?"
                            }
                            _streamingText.value = _streamingText.value +
                                "\n[tool: ${event.tool} $statusLabel]"
                        }
                        is AgentEvent.Completed -> {
                            // Persist the final assistant message.
                            val finalText = _streamingText.value.ifBlank { event.finalText }
                            val assistant = sessionRepo.appendMessage(sessionId, "assistant", finalText)
                            _messages.value = _messages.value + assistant
                            _streamingText.value = ""
                            sessionRepo.setStatus(sessionId, "success")
                        }
                        is AgentEvent.MaxIterationsReached -> {
                            val assistant = sessionRepo.appendMessage(
                                sessionId, "assistant",
                                "⚠ Max iterations (${event.max}) reached without a completion.",
                            )
                            _messages.value = _messages.value + assistant
                            _streamingText.value = ""
                            sessionRepo.setStatus(sessionId, "error")
                        }
                        is AgentEvent.Error -> {
                            val assistant = sessionRepo.appendMessage(
                                sessionId, "assistant",
                                "⚠ Error: ${event.message}",
                            )
                            _messages.value = _messages.value + assistant
                            _streamingText.value = ""
                            sessionRepo.setStatus(sessionId, "error")
                        }
                        is AgentEvent.IterationStarted -> Unit
                    }
                }
            }

            // Build the conversation history for the LLM (system + prior + new user).
            val projectName = AppContainer.activeProjectFolder?.lastPathSegment ?: "project"
            val systemPrompt = AppContainer.buildSystemPrompt(projectName)
            val priorMessages = (_messages.value - userMsg).map {
                ChatMessage(it.role, it.content)
            }
            val history = systemPrompt + priorMessages + ChatMessage("user", text)

            runJob = rt.run(
                scope = viewModelScope,
                history = history,
                onAssistantToken = { tok ->
                    // Handled via events — keep this no-op so the runtime can stream.
                    @Suppress("UNUSED_EXPRESSION") tok.toString()
                },
                onAssistantMessage = { fullText ->
                    if (fullText.isNotBlank() && _streamingText.value.isBlank()) {
                        _streamingText.value = fullText
                    }
                },
            )
            runJob?.join()
            eventJob.cancel()
            _isStreaming.value = false
        }
    }

    fun stop() {
        runtime?.stop()
        runJob?.cancel()
        runJob = null
        _isStreaming.value = false
        _streamingText.value = ""
    }

    private suspend fun ensureSession(): String? {
        activeSessionId?.let { return it }
        val project = projects.value.firstOrNull() ?: return null
        // Pick the most recent session in this project, or create one.
        val existing = sessionRepo.observeByProject(project.id)
        // Simpler: always create a fresh session on first send of a fresh VM.
        val session = sessionRepo.create(projectId = project.id, title = "Run")
        activeSessionId = session.id
        return session.id
    }

    override fun onCleared() {
        super.onCleared()
        runtime?.stop()
    }
}
