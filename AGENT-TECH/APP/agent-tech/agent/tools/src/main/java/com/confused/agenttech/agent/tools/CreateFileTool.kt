package com.confused.agenttech.agent.tools

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.confused.agenttech.common.Logger

/**
 * CreateFileTool — creates a new (or overwrites an existing) file inside
 * the project folder. Intermediate directories are created on demand.
 *
 * Input:
 *   - `path` (required): relative path including filename
 *   - `content` (required): UTF-8 text content
 */
class CreateFileTool(private val context: ToolContext) : Tool {

    override val name: String = "create_file"
    override val description: String =
        "Create (or overwrite) a file in the project folder. " +
        "Input: {\"path\": \"...\", \"content\": \"...\"}."

    override suspend fun execute(input: ToolInput): ToolResult {
        val root = context.projectRoot
            ?: return ToolResult(ToolResultStatus.ERROR, "No active project.", true)
        val path = input.require("path")
        val content = input.require("content")

        return try {
            val rootDoc = DocumentFile.fromTreeUri(context.appContext, root)
                ?: return ToolResult(ToolResultStatus.ERROR, "Invalid project root.", true)

            val segments = path.split('/').filter { it.isNotEmpty() }
            if (segments.isEmpty()) {
                return ToolResult(ToolResultStatus.ERROR, "Empty path.", true)
            }
            var current = rootDoc
            for (dirName in segments.dropLast(1)) {
                current = current.findFile(dirName) ?: current.createDirectory(dirName)!!
            }
            val fileName = segments.last()
            val existing = current.findFile(fileName)
            val target = existing ?: current.createFile("application/octet-stream", fileName)!!
            if (!target.isFile) {
                return ToolResult(ToolResultStatus.ERROR, "Not a file: $path", true)
            }

            val resolver = context.appContext.contentResolver
            resolver.openOutputStream(target.uri, "wt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            } ?: return ToolResult(ToolResultStatus.ERROR, "Could not open output stream.", true)

            Logger.d("Tool:CreateFile", "created $path (${content.length} chars)")
            ToolResult(ToolResultStatus.SUCCESS, "Created $path (${content.length} chars).")
        } catch (e: Exception) {
            Logger.w("Tool:CreateFile", "failed: ${e.message}", e)
            ToolResult(ToolResultStatus.ERROR, "Create failed: ${e.message}", true)
        }
    }
}
