package com.confused.onlylist.common

import android.util.Log

/**
 * Central Logger wrapper. Per CORE_RULES §20: never call Log.d() directly.
 * Tags are per-module: "OnlyList:Core:Database", "OnlyList:Feature:Details", etc.
 * Toggleable off in release; runtime toggle in Settings for debug builds.
 *
 * Usage: Logger.d("Feature:Home", "Loaded N items")
 */
object Logger {
    @Volatile
    var enabled: Boolean = true

    fun v(tag: String, message: String) {
        if (enabled) Log.v("OnlyList:$tag", message)
    }

    fun d(tag: String, message: String) {
        if (enabled) Log.d("OnlyList:$tag", message)
    }

    fun i(tag: String, message: String) {
        if (enabled) Log.i("OnlyList:$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) Log.w("OnlyList:$tag", message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) Log.e("OnlyList:$tag", message, throwable)
    }
}
