package com.confused.agenttech.agent.tools

import com.confused.agenttech.common.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FetchWebTool — fetches a URL via HTTP GET and returns the body as text.
 *
 * Used by the agent to pull docs / RFCs / online help into context.
 *
 * Input:
 *   - `url` (required): http(s) URL
 *   - `maxChars` (optional, default 8000): cap response length fed to the model
 */
class FetchWebTool : Tool {

    override val name: String = "fetch_web"
    override val description: String =
        "Fetch a URL via HTTP GET and return the body as text. " +
        "Input: {\"url\": \"https://...\", \"maxChars\": 8000}."

    private val client = HttpClient(OkHttp)

    override suspend fun execute(input: ToolInput): ToolResult {
        val url = input.require("url")
        val maxChars = input.optional("maxChars")?.toIntOrNull() ?: 8000

        return withContext(Dispatchers.IO) {
            runCatching {
                Logger.d("Tool:FetchWeb", "GET $url")
                val response = client.get(url)
                if (!response.status.isSuccess()) {
                    return@runCatching ToolResult(
                        ToolResultStatus.ERROR,
                        "HTTP ${response.status.value}",
                        true,
                    )
                }
                val text = response.bodyAsText()
                val truncated = if (text.length > maxChars) text.take(maxChars) + "\n…[truncated]" else text
                ToolResult(ToolResultStatus.SUCCESS, truncated)
            }.getOrElse {
                Logger.w("Tool:FetchWeb", "failed: ${it.message}", it)
                ToolResult(ToolResultStatus.ERROR, "Fetch failed: ${it.message}", true)
            }
        }
    }
}
