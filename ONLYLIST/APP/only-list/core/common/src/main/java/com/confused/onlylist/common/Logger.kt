package com.confused.onlylist.common

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central Logger wrapper. Per CORE_RULES §20: never call Log.d() directly.
 * Tags are per-module: "OnlyList:Core:Database", "OnlyList:Feature:Details", etc.
 * Toggleable off in release; runtime toggle in Settings for debug builds.
 *
 * Also maintains an in-memory ring buffer of recent log entries (for the
 * Logging screen in Settings). Per CORE_RULES §20: filtered by tag + level.
 */
object Logger {

    @Volatile
    var enabled: Boolean = true

    private val _logBuffer = MutableStateFlow<List<LogEntry>>(emptyList())
    val logBuffer: StateFlow<List<LogEntry>> = _logBuffer.asStateFlow()

    private val MAX_BUFFER_SIZE = 500

    fun v(tag: String, message: String) {
        addEntry(LogLevel.VERBOSE, tag, message)
        if (enabled) Log.v("OnlyList:$tag", message)
    }

    fun d(tag: String, message: String) {
        addEntry(LogLevel.DEBUG, tag, message)
        if (enabled) Log.d("OnlyList:$tag", message)
    }

    fun i(tag: String, message: String) {
        addEntry(LogLevel.INFO, tag, message)
        if (enabled) Log.i("OnlyList:$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        addEntry(LogLevel.WARN, tag, message, throwable?.message)
        if (enabled) Log.w("OnlyList:$tag", message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        addEntry(LogLevel.ERROR, tag, message, throwable?.message)
        if (enabled) Log.e("OnlyList:$tag", message, throwable)
    }

    private fun addEntry(level: LogLevel, tag: String, message: String, stackTrace: String? = null) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            stackTrace = stackTrace,
        )
        val current = _logBuffer.value
        val newList = (current + entry).takeLast(MAX_BUFFER_SIZE)
        _logBuffer.value = newList
    }

    fun clear() {
        _logBuffer.value = emptyList()
    }
}

enum class LogLevel(val priority: Int, val label: String) {
    VERBOSE(2, "V"),
    DEBUG(3, "D"),
    INFO(4, "I"),
    WARN(5, "W"),
    ERROR(6, "E");
}

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val stackTrace: String?,
)
