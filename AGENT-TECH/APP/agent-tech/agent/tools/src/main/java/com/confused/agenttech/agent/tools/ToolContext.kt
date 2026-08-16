package com.confused.agenttech.agent.tools

import android.content.Context
import android.net.Uri

/**
 * Context available to every [Tool] — the app context (for ContentResolver)
 * plus the SAF tree URI of the currently active project.
 *
 * Tools use [projectRoot] to resolve relative file paths to SAF document URIs.
 * A projectRoot of `null` (no active project) means file tools refuse to run.
 */
data class ToolContext(
    val appContext: Context,
    val projectRoot: Uri?,
)
