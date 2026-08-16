package com.confused.agenttech

import android.content.Context
import android.net.Uri
import com.confused.agenttech.agent.core.AgentRuntime
import com.confused.agenttech.agent.core.ContextManager
import com.confused.agenttech.agent.llm.ChatMessage
import com.confused.agenttech.agent.llm.LlmProvider
import com.confused.agenttech.agent.llm.OpenAiCompatibleProvider
import com.confused.agenttech.agent.storage.ProjectRepository
import com.confused.agenttech.agent.storage.ProviderRepository
import com.confused.agenttech.agent.storage.SessionRepository
import com.confused.agenttech.agent.storage.UsageRepository
import com.confused.agenttech.agent.tools.ToolContext
import com.confused.agenttech.agent.tools.ToolRegistry
import com.confused.agenttech.common.Logger
import com.confused.agenttech.database.DatabaseProvider

/**
 * Simple dependency container.
 *
 * Initialized in [AgentTechApplication.onCreate]. Holds singletons:
 * database, repositories, the [ToolRegistry], and a lazily-rebuilt
 * [AgentRuntime] for the currently active session.
 */
object AppContainer {

    private lateinit var appContext: Context

    val database by lazy { DatabaseProvider.get(appContext) }

    val projectRepository by lazy { ProjectRepository(database.projectDao()) }
    val sessionRepository by lazy {
        SessionRepository(database.sessionDao(), database.messageDao())
    }
    val providerRepository by lazy { ProviderRepository(database.providerDao()) }
    val usageRepository by lazy { UsageRepository(database.usageLogDao()) }

    /** Currently active project (folder URI). Null when no project selected. */
    @Volatile
    var activeProjectFolder: Uri? = null

    /** ToolRegistry — rebuilt when [activeProjectFolder] changes via [updateToolContext]. */
    @Volatile
    var toolRegistry: ToolRegistry? = null
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        // Seed default providers if none configured.
        kotlinx.coroutines.runBlocking {
            runCatching { providerRepository.seedDefaultsIfEmpty() }
                .onFailure { Logger.w("AppContainer", "seed defaults failed: ${it.message}", it) }
        }
        // Initialize toolRegistry with no project (file tools will refuse to run
        // until a project is set).
        toolRegistry = ToolRegistry(ToolContext(appContext, null))
    }

    /** Re-bind the tool registry to a new project folder URI. */
    fun updateToolContext(folderUri: Uri?) {
        activeProjectFolder = folderUri
        toolRegistry = ToolRegistry(ToolContext(appContext, folderUri))
        Logger.d("AppContainer", "tool context updated → $folderUri")
    }

    /** Build (or rebuild) a provider from the active ProviderEntity. */
    fun buildProvider(): LlmProvider? {
        val provider = kotlinx.coroutines.runBlocking { providerRepository.getActive() }
            ?: return null
        return OpenAiCompatibleProvider(
            displayName = "${provider.name} / ${provider.modelName}",
            baseUrl = provider.baseUrl,
            apiKey = provider.apiKey,
            modelName = provider.modelName,
            temperature = provider.temperature,
            maxTokens = provider.maxTokens,
        )
    }

    /** Build a fresh AgentRuntime bound to the currently active provider. */
    fun buildRuntime(): AgentRuntime? {
        val provider = buildProvider() ?: return null
        val registry = toolRegistry ?: return null
        return AgentRuntime(
            provider = provider,
            toolRegistry = registry,
            contextManager = ContextManager(),
            maxIterations = 25,
        )
    }

    /** Convenience: build a [ChatMessage] list seeded with the system prompt. */
    fun buildSystemPrompt(projectName: String): List<ChatMessage> = listOf(
        ChatMessage(
            role = "system",
            content = """
                You are Agent Tech, a coding assistant that operates inside the user's
                project folder "$projectName". You can read, edit, create, list, and
                search files via the tools available to you. When you need clarification,
                call the ask_user tool. When the task is complete, call attempt_completion
                with a brief summary.

                To call a tool, end your message with a JSON block:
                ```json
                { "tool": "<tool_name>", "arguments": { "<arg>": "<value>" } }
                ```
            """.trimIndent(),
        ),
    )
}
