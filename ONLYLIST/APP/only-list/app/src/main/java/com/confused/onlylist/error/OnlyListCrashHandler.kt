package com.confused.onlylist.error

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global crash handler — per CORE_RULES §19.
 * Installed FIRST in Application.onCreate().
 * Persists crash report to filesDir/last_crash.txt, then launches ErrorActivity.
 */
class OnlyListCrashHandler(
    private val context: Context,
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val timestamp = dateFormat.format(Date())
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val report = buildString {
            appendLine("=== Only-List Crash Report ===")
            appendLine("Timestamp: $timestamp")
            appendLine("Thread: ${thread.name} (id=${thread.id})")
            appendLine("Process: ${Process.myPid()}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App version: ${getAppVersion()}")
            appendLine()
            appendLine("Exception: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message}")
            appendLine()
            appendLine("Stack Trace:")
            appendLine(stackTrace)
        }

        // Persist to file (survives process restart)
        try {
            File(context.filesDir, "last_crash.txt").writeText(report)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash report", e)
        }

        Log.e(TAG, "Uncaught exception — launching ErrorActivity", throwable)

        // Launch ErrorActivity
        try {
            val intent = Intent(context, ErrorActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch ErrorActivity", e)
        }

        // Kill the process
        Process.killProcess(Process.myPid())
        System.exit(1)
    }

    private fun getAppVersion(): String {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }

    companion object {
        private const val TAG = "OnlyList:Error:CrashHandler"
    }
}
