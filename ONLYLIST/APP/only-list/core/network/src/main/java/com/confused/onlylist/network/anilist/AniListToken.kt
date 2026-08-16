package com.confused.onlylist.network.anilist

import android.net.Uri

/**
 * Parses the OAuth redirect URI from AniList (Implicit Grant).
 * The redirect looks like: `olink://anilist-auth#access_token=xxx&expires_in=31536000&token_type=bearer`
 * The token is in the FRAGMENT (after #), not the query string.
 */
data class AniListToken(
    val accessToken: String,
    val expiresInSeconds: Long,
    val tokenType: String,
) {
    val expiresAtMillis: Long
        get() = System.currentTimeMillis() + expiresInSeconds * 1000

    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAtMillis

    companion object {
        /**
         * Parse the token from a redirect URI.
         * Returns null if the URI doesn't contain a valid token.
         */
        fun fromRedirectUri(uri: Uri): AniListToken? {
            if (uri.scheme != "olink" || uri.host != "anilist-auth") return null
            val fragment = uri.fragment ?: return null
            val params = fragment.split("&").mapNotNull { pair ->
                val idx = pair.indexOf("=")
                if (idx > 0) pair.substring(0, idx) to pair.substring(idx + 1)
                else null
            }.toMap()

            val accessToken = params["access_token"] ?: return null
            val expiresIn = params["expires_in"]?.toLongOrNull() ?: 31536000L
            val tokenType = params["token_type"] ?: "bearer"
            return AniListToken(accessToken, expiresIn, tokenType)
        }
    }
}
