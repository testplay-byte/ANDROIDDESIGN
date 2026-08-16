package com.confused.onlylist

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.crossfade
import com.confused.onlylist.common.Logger
import com.confused.onlylist.error.OnlyListCrashHandler

/**
 * Application entry point. Per CORE_RULES §19: the global crash handler is
 * installed FIRST (before Logger or any other init).
 */
class OnlyListApplication : Application(), coil3.SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        // Install crash handler FIRST — before any other init
        Thread.setDefaultUncaughtExceptionHandler(OnlyListCrashHandler(this))
        // Initialize the DI container
        AppContainer.init(this)
        Logger.enabled = BuildConfig.DEBUG
        Logger.i("App", "Only-List ${BuildConfig.VERSION_NAME} started")
    }

    // R-13 Performance: configure Coil with crossfade for smooth image loading.
    // Coil 3 has built-in memory + disk cache by default.
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}
