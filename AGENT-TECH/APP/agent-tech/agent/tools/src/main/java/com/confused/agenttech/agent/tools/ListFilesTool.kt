package com.confused.agenttech.agent.tools

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.confused.agenttech.common.Logger

/**
 * ListFilesTool — lists files in a directory under the project root.
 *
 * Input:
 *   - `path` (optional, defaults to root): relative path of a directory.
 *
 * Output: a newline-separated list of entries. Directories are suffixed with "/".
 */
class ListFilesTool(private val context: ToolContext) : Tool {

    override val name: String = "list_files"
    override val description: String =
        "List files in a directory under the project root. " +
        "Input: {\"path\": \"src/main/kotlin\"}. Directories are suffixed with '/'."

    override suspend fun execute(input: ToolInput): ToolResult {
        val root = context.projectRoot
            ?: return ToolResult(ToolResultStatus.ERROR, "No active project.", true)
        val relativePath = input.optional("path") ?: ""

        return try {
            val rootDoc = DocumentFile.fromTreeUri(context.appContext, root)
                ?: return ToolResult(ToolResultStatus.ERROR, "Invalid project root.", true)

            val target = if (relativePath.isBlank()) rootDoc else {
                val segments = relativePath.split('/').filter { it.isNotEmpty() }
                var current = rootDoc
                for (s in segments) {
                    current = current.findFile(s)
                        ?: return ToolResult(ToolResultStatus.ERROR, "Not found: $relativePath", true)
                }
                current
            }
            if (!target.isDirectory) {
                return ToolResult(ToolResultStatus.ERROR, "Not a directory: $relativePath", true)
            }
            val listing = target.listFiles().joinToString(separator = "\n") { f ->
                val name = f.name ?: "(unknown)"
                if (f.isDirectory) "$name/" else name
            }
            Logger.d("Tool:ListFiles", "listed $relativePath (${target.listFiles().size} entries)")
            ToolResult(ToolResultStatus.SUCCESS, listing.ifEmpty { "(empty)" })
        } catch (e: Exception) {
            Logger.w("Tool:ListFiles", "failed: ${e.message}", e)
            ToolResult(ToolResultStatus.ERROR, "List failed: ${e.message}", true)
        }
    }
}
