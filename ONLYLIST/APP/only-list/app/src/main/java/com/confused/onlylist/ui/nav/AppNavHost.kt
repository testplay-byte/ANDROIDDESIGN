package com.confused.onlylist.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.confused.onlylist.designsystem.components.OnlyListBottomBar
import com.confused.onlylist.ui.screens.airing.AiringScreen
import com.confused.onlylist.ui.screens.details.DetailsScreen
import com.confused.onlylist.ui.screens.home.HomeScreen
import com.confused.onlylist.ui.screens.library.LibraryScreen
import com.confused.onlylist.ui.screens.search.SearchScreen
import com.confused.onlylist.ui.screens.settings.SettingsScreen
import dev.chrisbanes.haze.HazeState

/**
 * Single shared HazeState for the whole app.
 *
 * R-11 FIX: The previous design had each screen create its OWN HazeState for
 * its LazyColumn + CollapsibleHeader, and a SEPARATE bottomBarHazeState in
 * AppNavHost for the bottom bar. But no composable was marked as the SOURCE
 * for bottomBarHazeState — so Haze had nothing to sample → no blur, just tint.
 *
 * The fix: ONE shared HazeState. Each screen marks its LazyColumn with
 * Modifier.haze(sharedHazeState) (the blur source). The bottom bar + header
 * consume via hazeChild(sharedHazeState). Haze samples whichever composable
 * is currently visible behind the bar/header.
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val sharedHazeState = remember { HazeState() }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Destinations.HOME) {
                HomeScreen(hazeState = sharedHazeState)
            }
            composable(Destinations.SEARCH) {
                SearchScreen(hazeState = sharedHazeState)
            }
            composable(Destinations.AIRING) {
                AiringScreen(hazeState = sharedHazeState)
            }
            composable(Destinations.LIBRARY) {
                LibraryScreen(
                    hazeState = sharedHazeState,
                    onMediaClick = { mediaId ->
                        navController.navigate(Destinations.details(mediaId))
                    },
                )
            }
            composable(Destinations.SETTINGS) {
                SettingsScreen(hazeState = sharedHazeState)
            }
            composable(
                route = Destinations.DETAILS,
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.IntType }
                ),
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getInt("mediaId") ?: 0
                DetailsScreen(
                    mediaId = mediaId,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // Floating pill bottom nav overlays the content.
        // Uses the SAME sharedHazeState as the screens — so it blurs whatever
        // LazyColumn is currently visible behind it.
        if (currentRoute in Destinations.bottomNavRoutes) {
            OnlyListBottomBar(
                currentRoute = currentRoute ?: Destinations.HOME,
                onNavigate = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                hazeState = sharedHazeState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
