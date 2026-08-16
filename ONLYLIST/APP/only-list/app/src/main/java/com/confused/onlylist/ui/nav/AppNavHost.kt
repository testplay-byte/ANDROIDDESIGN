package com.confused.onlylist.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.confused.onlylist.designsystem.components.OnlyListBottomBar
import com.confused.onlylist.ui.screens.airing.AiringScreen
import com.confused.onlylist.ui.screens.details.DetailsScreen
import com.confused.onlylist.ui.screens.home.HomeScreen
import com.confused.onlylist.ui.screens.library.LibraryScreen
import com.confused.onlylist.ui.screens.search.SearchScreen
import com.confused.onlylist.ui.screens.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
        ) {
            composable(Destinations.HOME) {
                HomeScreen()
            }
            composable(Destinations.SEARCH) {
                SearchScreen()
            }
            composable(Destinations.AIRING) {
                AiringScreen()
            }
            composable(Destinations.LIBRARY) {
                LibraryScreen(
                    onMediaClick = { mediaId ->
                        navController.navigate(Destinations.details(mediaId))
                    },
                )
            }
            composable(Destinations.SETTINGS) {
                SettingsScreen()
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
        // Hidden on Details screen (it's a detail view, not a tab).
        val currentRoute = navController.currentBackStackEntry?.destination?.route
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
            )
        }
    }
}
