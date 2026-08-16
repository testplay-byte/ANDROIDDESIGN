package com.confused.onlylist

import android.app.Application
import com.confused.onlylist.common.Logger

/**
 * Application entry point. Per CORE_RULES §19: the global crash handler would be
 * installed here FIRST (Phase 2). v1 just initializes the Logger.
 */
class OnlyListApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.enabled = BuildConfig.DEBUG
        Logger.i("App", "Only-List ${BuildConfig.VERSION_NAME} started")
    }
}
