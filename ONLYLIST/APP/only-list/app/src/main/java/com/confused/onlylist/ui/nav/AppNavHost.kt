package com.confused.onlylist.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    // FIX: use currentBackStackEntryAsState() so the route is REACTIVE —
    // recomposes when navigation changes, so the bottom bar selection updates.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier.fillMaxSize(),
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
        // FIX: align to BottomCenter so it appears at the BOTTOM, not the top.
        // Hidden on Details screen (it's a detail view, not a tab).
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
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
