package com.confused.agenttech.agent.tools

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.confused.agenttech.common.Logger

/**
 * ReadFileTool — reads a UTF-8 text file from the active project folder.
 *
 * Input:
 *   - `path` (required): forward-slash-separated path relative to the project root.
 *
 * Output: the file contents as a UTF-8 string (truncated to 200KB for the
 * assistant's view — full content is read but the model sees a bounded slice).
 */
class ReadFileTool(private val context: ToolContext) : Tool {

    override val name: String = "read_file"
    override val description: String =
        "Read a UTF-8 text file from the project folder. Input: {\"path\": \"relative/path/to/file.kt\"}."

    override suspend fun execute(input: ToolInput): ToolResult {
        val root = context.projectRoot
            ?: return ToolResult(ToolResultStatus.ERROR, "No active project.", true)
        val path = input.require("path")
        val file = findInTree(root, path)
            ?: return ToolResult(ToolResultStatus.ERROR, "File not found: $path", true)

        return try {
            val resolver = context.appContext.contentResolver
            val text = resolver.openInputStream(file.uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: return ToolResult(ToolResultStatus.ERROR, "Could not open file: $path", true)

            Logger.d("Tool:ReadFile", "read $path (${text.length} chars)")
            ToolResult(ToolResultStatus.SUCCESS, text)
        } catch (e: Exception) {
            Logger.w("Tool:ReadFile", "failed: ${e.message}", e)
            ToolResult(ToolResultStatus.ERROR, "Read failed: ${e.message}", true)
        }
    }

    private fun findInTree(rootUri: Uri, relativePath: String): DocumentFile? {
        val root = DocumentFile.fromTreeUri(context.appContext, rootUri) ?: return null
        val segments = relativePath.split('/').filter { it.isNotEmpty() }
        var current: DocumentFile = root
        for (segment in segments) {
            current = current.findFile(segment) ?: return null
        }
        return current
    }
}
