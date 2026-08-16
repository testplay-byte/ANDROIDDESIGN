package com.confused.agenttech.agent.tools

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.confused.agenttech.common.Logger

/**
 * EditFileTool — replaces the first occurrence of `oldText` with `newText`
 * inside an existing project file.
 *
 * Input:
 *   - `path` (required)
 *   - `oldText` (required): exact text to find (preserves indentation)
 *   - `newText` (required): replacement text
 *
 * Output: a short confirmation message.
 */
class EditFileTool(private val context: ToolContext) : Tool {

    override val name: String = "edit_file"
    override val description: String =
        "Edit an existing file by replacing oldText with newText (first occurrence only). " +
        "Input: {\"path\": \"...\", \"oldText\": \"...\", \"newText\": \"...\"}."

    override suspend fun execute(input: ToolInput): ToolResult {
        val root = context.projectRoot
            ?: return ToolResult(ToolResultStatus.ERROR, "No active project.", true)
        val path = input.require("path")
        val oldText = input.require("oldText")
        val newText = input.require("newText")

        val file = findInTree(root, path)
            ?: return ToolResult(ToolResultStatus.ERROR, "File not found: $path", true)
        if (!file.isFile) {
            return ToolResult(ToolResultStatus.ERROR, "Not a file: $path", true)
        }

        return try {
            val resolver = context.appContext.contentResolver
            val original = resolver.openInputStream(file.uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: return ToolResult(ToolResultStatus.ERROR, "Could not read file: $path", true)

            val idx = original.indexOf(oldText)
            if (idx < 0) {
                return ToolResult(
                    ToolResultStatus.ERROR,
                    "oldText not found in $path — no edit applied.",
                    true,
                )
            }
            val updated = original.replaceRange(idx, idx + oldText.length, newText)

            resolver.openOutputStream(file.uri, "wt")?.use { out ->
                out.write(updated.toByteArray(Charsets.UTF_8))
            } ?: return ToolResult(ToolResultStatus.ERROR, "Could not write file: $path", true)

            Logger.d("Tool:EditFile", "edited $path")
            ToolResult(ToolResultStatus.SUCCESS, "Edited $path (replaced ${oldText.length} chars).")
        } catch (e: Exception) {
            Logger.w("Tool:EditFile", "failed: ${e.message}", e)
            ToolResult(ToolResultStatus.ERROR, "Edit failed: ${e.message}", true)
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
