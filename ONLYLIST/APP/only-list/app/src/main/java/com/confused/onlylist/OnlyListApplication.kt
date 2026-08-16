package com.confused.onlylist

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.confused.onlylist.common.Logger
import com.confused.onlylist.error.OnlyListCrashHandler
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

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

    // R-13 Performance: configure Coil with memory + disk cache.
    // This speeds up image loading significantly (no re-downloading).
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(0.25)  // 25% of app memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024)  // 250MB
                    .build()
            }
            .crossfade(true)  // smooth fade-in for images
            .build()
    }
}
