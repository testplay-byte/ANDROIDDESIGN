package com.confused.onlylist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import com.confused.onlylist.common.Logger
import com.confused.onlylist.designsystem.theme.AppTheme
import com.confused.onlylist.network.anilist.AniListConfig
import com.confused.onlylist.ui.nav.AppNavHost

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                AppNavHost()
            }
        }
        // Handle AniList OAuth redirect (if launched via deep link)
        intent?.let { handleAuthRedirect(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthRedirect(intent)
    }

    private fun handleAuthRedirect(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme == "olink" && data.host == "anilist-auth") {
            val success = AppContainer.authManager.handleRedirectUri(data)
            if (success) {
                Logger.i("Auth", "AniList OAuth token stored successfully")
            } else {
                Logger.w("Auth", "Failed to parse AniList OAuth redirect")
            }
        }
    }

    companion object {
        /**
         * Opens the AniList OAuth authorization page in Chrome Custom Tabs.
         * After the user authorizes, AniList redirects to olink://anilist-auth
         * which is caught by this Activity's intent-filter.
         */
        fun startAniListAuth(context: android.content.Context) {
            val url = Uri.parse(AniListConfig.authUrl())
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, url)
        }
    }
}
