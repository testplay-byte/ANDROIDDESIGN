package com.confused.agenttech.agent.tools

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.confused.agenttech.common.Logger
import java.util.regex.Pattern

/**
 * SearchFilesTool — searches file contents by Java regex across the project.
 *
 * Per R-A7 ("walk-on-demand"): we do NOT maintain a full-text index. On every
 * invocation we walk the SAF tree and grep each text-looking file (small + non-binary).
 *
 * This is O(N files × M size) per call but for typical projects (<1000 files,
 * <100KB each) it completes in well under a second on a mid-range device.
 *
 * Input:
 *   - `query` (required): Java regex pattern
 *   - `path` (optional): restrict search to a subtree (relative path)
 *   - `maxResults` (optional, default 50): cap matches returned
 */
class SearchFilesTool(private val context: ToolContext) : Tool {

    override val name: String = "search_files"
    override val description: String =
        "Search file contents by Java regex across the project. " +
        "Input: {\"query\": \"fun .*\\\\(\", \"path\": \"src\", \"maxResults\": 50}."

    override suspend fun execute(input: ToolInput): ToolResult {
        val root = context.projectRoot
            ?: return ToolResult(ToolResultStatus.ERROR, "No active project.", true)
        val query = input.require("query")
        val subPath = input.optional("path") ?: ""
        val maxResults = input.optional("maxResults")?.toIntOrNull() ?: 50

        val pattern: Pattern = try {
            Pattern.compile(query)
        } catch (e: Exception) {
            return ToolResult(ToolResultStatus.ERROR, "Invalid regex: ${e.message}", true)
        }

        return try {
            val rootDoc = DocumentFile.fromTreeUri(context.appContext, root)
                ?: return ToolResult(ToolResultStatus.ERROR, "Invalid project root.", true)
            val searchRoot = if (subPath.isBlank()) rootDoc else {
                val segments = subPath.split('/').filter { it.isNotEmpty() }
                var current = rootDoc
                for (s in segments) {
                    current = current.findFile(s)
                        ?: return ToolResult(ToolResultStatus.ERROR, "Not found: $subPath", true)
                }
                current
            }

            val results = StringBuilder()
            var count = 0
            walkAndGrep(searchRoot, "", pattern, maxResults, results) { count++ }
            Logger.d("Tool:SearchFiles", "matched $count lines for /$query/")
            if (count == 0) {
                ToolResult(ToolResultStatus.SUCCESS, "No matches found for /$query/.")
            } else {
                ToolResult(ToolResultStatus.SUCCESS, results.toString())
            }
        } catch (e: Exception) {
            Logger.w("Tool:SearchFiles", "failed: ${e.message}", e)
            ToolResult(ToolResultStatus.ERROR, "Search failed: ${e.message}", true)
        }
    }

    private fun walkAndGrep(
        node: DocumentFile,
        accumulatedPath: String,
        pattern: Pattern,
        maxResults: Int,
        out: StringBuilder,
        onMatch: () -> Unit,
    ) {
        if (out.length > 32_000) return // bound output size
        if (node.isDirectory) {
            for (child in node.listFiles()) {
                val childName = child.name ?: continue
                val childPath = if (accumulatedPath.isEmpty()) childName else "$accumulatedPath/$childName"
                walkAndGrep(child, childPath, pattern, maxResults, out, onMatch)
            }
        } else if (node.isFile && looksTextual(node.name)) {
            val resolver = context.appContext.contentResolver
            val text = try {
                resolver.openInputStream(node.uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.size > 512 * 1024) return // skip huge files
                    bytes.toString(Charsets.UTF_8)
                } ?: return
            } catch (e: Exception) {
                return
            }
            val matcher = pattern.matcher(text)
            var lineStart = 0
            var lineNum = 1
            var matchCount = 0
            for (i in text.indices) {
                if (text[i] == '\n') {
                    val line = text.substring(lineStart, i)
                    if (matcher.region(lineStart, i).find()) {
                        out.append("$accumulatedPath:$lineNum: $line\n")
                        onMatch()
                        matchCount++
                        if (matchCount >= maxResults) return
                    }
                    lineStart = i + 1
                    lineNum++
                }
            }
            if (lineStart < text.length) {
                val line = text.substring(lineStart)
                if (matcher.region(lineStart, text.length).find()) {
                    out.append("$accumulatedPath:$lineNum: $line\n")
                    onMatch()
                }
            }
        }
    }

    private fun looksTextual(name: String?): Boolean {
        if (name == null) return false
        val textual = listOf(
            ".kt", ".java", ".kts", ".gradle",
            ".xml", ".json", ".yaml", ".yml", ".toml",
            ".md", ".txt", ".rst",
            ".py", ".js", ".ts", ".tsx", ".jsx",
            ".c", ".cpp", ".h", ".hpp", ".rs", ".go", ".rb", ".php",
            ".sh", ".bash", ".zsh",
            ".sql", ".csv",
            ".env", ".ini", ".cfg", ".conf",
            ".gitignore", ".properties",
        )
        return textual.any { name.endsWith(it, ignoreCase = true) }
    }
}
