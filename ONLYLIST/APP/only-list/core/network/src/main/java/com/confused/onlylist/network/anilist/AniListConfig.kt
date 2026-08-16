package com.confused.onlylist.network.anilist

/**
 * AniList configuration.
 * Client ID is public (not a secret) — shown in AniList developer settings.
 * Client Secret is NOT stored here (Implicit Grant doesn't need it).
 */
object AniListConfig {
    const val CLIENT_ID = "48704"
    const val REDIRECT_URI = "olink://anilist-auth"
    const val AUTH_URL = "https://anilist.co/api/v2/oauth/authorize"
    const val API_URL = "https://graphql.anilist.co"

    /**
     * Builds the OAuth authorization URL for Implicit Grant.
     * The user opens this in Chrome Custom Tabs; after authorizing,
     * AniList redirects to REDIRECT_URI with the token in the URL fragment.
     */
    fun authUrl(): String = buildString {
        append(AUTH_URL)
        append("?client_id=").append(CLIENT_ID)
        append("&redirect_uri=").append(REDIRECT_URI)
        append("&response_type=token")
    }
}
