package com.confused.onlylist.network.anilist

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

/**
 * Manages the AniList OAuth token (Implicit Grant).
 * v1 stores in SharedPreferences (plaintext) — Phase 5 will add Keystore encryption.
 * Per CORE_RULES §14 rule 6: token in Keystore (encrypted), never logged.
 *
 * TODO(Phase 5): encrypt the token with Android Keystore.
 */
class AniListAuthManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var cachedToken: AniListToken? = null

    val isLoggedIn: Boolean
        get() = getToken() != null

    /**
     * Returns the access token, or null if not logged in / expired.
     */
    fun getToken(): String? {
        cachedToken?.let { token ->
            if (!token.isExpired) return token.accessToken
            // Token expired — clear it
            logout()
        }
        // Try loading from prefs
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        if (expiresAt > 0 && System.currentTimeMillis() >= expiresAt) {
            logout()
            return null
        }
        cachedToken = AniListToken(
            accessToken = accessToken,
            expiresInSeconds = if (expiresAt > 0) (expiresAt - System.currentTimeMillis()) / 1000 else 0,
            tokenType = "bearer",
        )
        return accessToken
    }

    /**
     * Stores the token from an OAuth redirect URI.
     * Returns true if the token was parsed + stored successfully.
     */
    fun handleRedirectUri(uri: Uri): Boolean {
        val token = AniListToken.fromRedirectUri(uri) ?: return false
        cachedToken = token
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token.accessToken)
            .putLong(KEY_EXPIRES_AT, token.expiresAtMillis)
            .apply()
        return true
    }

    fun logout() {
        cachedToken = null
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "anilist_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
