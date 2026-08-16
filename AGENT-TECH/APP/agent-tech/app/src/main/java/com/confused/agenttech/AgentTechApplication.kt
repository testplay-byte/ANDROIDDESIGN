package com.confused.agenttech

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.crossfade
import com.confused.agenttech.common.Logger

/**
 * Application entry point. Initializes [AppContainer] and configures Coil.
 */
class AgentTechApplication : Application(), coil3.SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
        Logger.enabled = BuildConfig.DEBUG
        Logger.i("App", "Agent Tech ${BuildConfig.VERSION_NAME} started")
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}
