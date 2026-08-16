package com.confused.onlylist.ui.nav

/**
 * Navigation destinations. Each screen is a route.
 * Bottom nav tabs: Home, Search, Airing, Library, Settings (5).
 * Details is a navigated screen (not a tab): details/{mediaId}.
 */
object Destinations {
    const val HOME = "home"
    const val SEARCH = "search"
    const val AIRING = "airing"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val LOGS = "logs"
    const val DETAILS = "details/{mediaId}"

    fun details(mediaId: Int): String = "details/$mediaId"

    /** Routes that appear in the bottom nav (in order). */
    val bottomNavRoutes = listOf(HOME, SEARCH, AIRING, LIBRARY, SETTINGS)
}
