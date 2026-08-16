package com.confused.agenttech.agent.tools

import com.confused.agenttech.common.Logger

/**
 * ToolRegistry — registers every [Tool] the agent can invoke and dispatches
 * by name.
 *
 * Tools that need the active project's SAF context ([ReadFileTool],
 * [EditFileTool], [CreateFileTool], [ListFilesTool], [SearchFilesTool])
 * are registered fresh whenever the active project changes via [updateContext].
 *
 * Stateless tools ([FetchWebTool], [AskUserTool], [AttemptCompletionTool])
 * are registered once.
 */
class ToolRegistry(initialContext: ToolContext) {

    private var toolContext: ToolContext = initialContext
    private val toolsByName: MutableMap<String, Tool> = mutableMapOf()

    val askUserTool: AskUserTool = AskUserTool()

    init {
        registerStateless()
        reregisterWithContext(initialContext)
    }

    fun updateContext(newContext: ToolContext) {
        toolContext = newContext
        reregisterWithContext(newContext)
    }

    fun list(): List<Tool> = toolsByName.values.toList()

    fun byName(name: String): Tool? = toolsByName[name]

    suspend fun execute(name: String, input: ToolInput): ToolResult {
        val tool = toolsByName[name]
            ?: return ToolResult(ToolResultStatus.ERROR, "Unknown tool: $name", true)
        return runCatching { tool.execute(input) }
            .getOrElse {
                Logger.w("ToolRegistry", "tool $name threw: ${it.message}", it)
                ToolResult(ToolResultStatus.ERROR, "Tool $name threw: ${it.message}", true)
            }
    }

    private fun registerStateless() {
        register(FetchWebTool())
        register(askUserTool)
        register(AttemptCompletionTool())
    }

    private fun reregisterWithContext(context: ToolContext) {
        register(ReadFileTool(context))
        register(EditFileTool(context))
        register(CreateFileTool(context))
        register(ListFilesTool(context))
        register(SearchFilesTool(context))
    }

    private fun register(tool: Tool) {
        toolsByName[tool.name] = tool
    }
}
