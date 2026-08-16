package com.confused.agenttech.ui.nav

/**
 * Navigation destinations. Each screen is a route.
 * Bottom nav tabs: Chat, Files, Runs, Settings (4).
 * Other screens are navigated-to (not tabs).
 */
object Destinations {
    const val ONBOARDING = "onboarding"
    const val PROJECTS = "projects"
    const val CHAT = "chat"
    const val FILES = "files"
    const val RUNS = "runs"
    const val SETTINGS = "settings"
    const val PROVIDER_CONFIG = "provider_config"
    const val USAGE = "usage"

    /** Routes that appear in the bottom nav (in order). */
    val bottomNavRoutes = listOf(CHAT, FILES, RUNS, SETTINGS)

    /** Routes that show the floating bottom nav overlay. */
    val mainRoutes = setOf(CHAT, FILES, RUNS, SETTINGS)
}
