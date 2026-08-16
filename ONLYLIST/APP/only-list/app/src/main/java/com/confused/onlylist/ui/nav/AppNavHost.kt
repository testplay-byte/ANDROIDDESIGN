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

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // Shared HazeState — the bottom bar reads from whatever screen is visible.
    // Each screen also has its own HazeState for its LazyColumn + header; the bar
    // gets a screen-level hazeState passed via the composable() lambda below.
    val bottomBarHazeState = remember { HazeState() }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Destinations.HOME) {
                HomeScreen(bottomBarHazeState = bottomBarHazeState)
            }
            composable(Destinations.SEARCH) {
                SearchScreen(bottomBarHazeState = bottomBarHazeState)
            }
            composable(Destinations.AIRING) {
                AiringScreen(bottomBarHazeState = bottomBarHazeState)
            }
            composable(Destinations.LIBRARY) {
                LibraryScreen(
                    bottomBarHazeState = bottomBarHazeState,
                    onMediaClick = { mediaId ->
                        navController.navigate(Destinations.details(mediaId))
                    },
                )
            }
            composable(Destinations.SETTINGS) {
                SettingsScreen(bottomBarHazeState = bottomBarHazeState)
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
                hazeState = bottomBarHazeState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
