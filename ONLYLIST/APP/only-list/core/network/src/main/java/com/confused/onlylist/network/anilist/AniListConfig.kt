package com.confused.onlylist.network.anilist

/**
 * AniList configuration.
 * Client ID is public (not a secret) — shown in AniList developer settings.
 * Client Secret is NOT stored here (Implicit Grant doesn't need it).
 *
 * The redirect URI is configured in AniList developer settings (olink://anilist-auth).
 * Per the AniList Implicit Grant docs (https://docs.anilist.co/guide/auth/implicit),
 * the redirect_uri is NOT passed in the authorize URL — AniList uses the one from
 * the app settings. Passing it caused "unsupported_grant_type" errors.
 */
object AniListConfig {
    const val CLIENT_ID = "48704"
    const val REDIRECT_URI = "olink://anilist-auth"
    const val AUTH_URL = "https://anilist.co/api/v2/oauth/authorize"
    const val API_URL = "https://graphql.anilist.co"

    /**
     * Builds the OAuth authorization URL for Implicit Grant.
     * Per AniList docs: only client_id + response_type=token are passed.
     * The redirect_uri comes from the app's AniList developer settings.
     */
    fun authUrl(): String = buildString {
        append(AUTH_URL)
        append("?client_id=").append(CLIENT_ID)
        append("&response_type=token")
    }
}
