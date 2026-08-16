package com.confused.onlylist

import android.app.Application
import com.confused.onlylist.common.Logger
import com.confused.onlylist.error.OnlyListCrashHandler

/**
 * Application entry point. Per CORE_RULES §19: the global crash handler is
 * installed FIRST (before Logger or any other init).
 */
class OnlyListApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Install crash handler FIRST — before any other init
        Thread.setDefaultUncaughtExceptionHandler(OnlyListCrashHandler(this))
        Logger.enabled = BuildConfig.DEBUG
        Logger.i("App", "Only-List ${BuildConfig.VERSION_NAME} started")
    }
}
