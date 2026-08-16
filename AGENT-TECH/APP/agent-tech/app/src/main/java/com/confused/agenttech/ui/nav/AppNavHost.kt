package com.confused.agenttech.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.confused.agenttech.AppContainer
import com.confused.agenttech.designsystem.components.AgentBottomBar
import com.confused.agenttech.designsystem.components.BottomNavItem
import com.confused.agenttech.designsystem.R
import com.confused.agenttech.ui.screens.chat.ChatScreen
import com.confused.agenttech.ui.screens.files.FilesScreen
import com.confused.agenttech.ui.screens.onboarding.OnboardingScreen
import com.confused.agenttech.ui.screens.projects.ProjectsScreen
import com.confused.agenttech.ui.screens.runs.RunsScreen
import com.confused.agenttech.ui.screens.settings.ProviderConfigScreen
import com.confused.agenttech.ui.screens.settings.SettingsScreen
import com.confused.agenttech.ui.screens.usage.UsageScreen
import dev.chrisbanes.haze.HazeState

/**
 * Single shared HazeState for the whole app.
 * Each screen marks its LazyColumn with Modifier.haze(sharedHazeState) (the blur source).
 * The bottom bar + header consume via hazeChild(sharedHazeState).
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val sharedHazeState = remember { HazeState() }

    val projects by AppContainer.projectRepository.observeAll()
        .collectAsState(initial = emptyList())

    // Auto-route: if we're on the onboarding screen and projects exist, skip
    // to the Projects selector. (Onboarding only shows on first launch.)
    LaunchedEffect(projects.size) {
        if (projects.isNotEmpty() && currentRoute == Destinations.ONBOARDING) {
            navController.navigate(Destinations.PROJECTS) {
                popUpTo(Destinations.ONBOARDING) { inclusive = true }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Destinations.ONBOARDING,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Destinations.ONBOARDING) {
                OnboardingScreen(
                    onProjectAdded = {
                        navController.navigate(Destinations.PROJECTS) {
                            popUpTo(Destinations.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Destinations.PROJECTS) {
                ProjectsScreen(
                    hazeState = sharedHazeState,
                    onProjectOpened = {
                        navController.navigate(Destinations.CHAT) {
                            popUpTo(Destinations.PROJECTS) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Destinations.CHAT) {
                ChatScreen(hazeState = sharedHazeState)
            }
            composable(Destinations.FILES) {
                FilesScreen(hazeState = sharedHazeState)
            }
            composable(Destinations.RUNS) {
                RunsScreen(hazeState = sharedHazeState)
            }
            composable(Destinations.SETTINGS) {
                SettingsScreen(
                    hazeState = sharedHazeState,
                    onNavigateToProviderConfig = { navController.navigate(Destinations.PROVIDER_CONFIG) },
                    onNavigateToUsage = { navController.navigate(Destinations.USAGE) },
                )
            }
            composable(Destinations.PROVIDER_CONFIG) {
                ProviderConfigScreen(
                    hazeState = sharedHazeState,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destinations.USAGE) {
                UsageScreen(
                    hazeState = sharedHazeState,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // Floating pill bottom nav overlays the content on main routes.
        if (currentRoute in Destinations.mainRoutes) {
            AgentBottomBar(
                currentRoute = currentRoute ?: Destinations.CHAT,
                onNavigate = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            // Pop back to PROJECTS (the de-facto root once onboarding is dismissed).
                            popUpTo(Destinations.PROJECTS) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                hazeState = sharedHazeState,
                modifier = Modifier.align(Alignment.BottomCenter),
                items = listOf(
                    BottomNavItem(Destinations.CHAT, R.drawable.ic_chat, "Chat"),
                    BottomNavItem(Destinations.FILES, R.drawable.ic_files, "Files"),
                    BottomNavItem(Destinations.RUNS, R.drawable.ic_runs, "Runs"),
                    BottomNavItem(Destinations.SETTINGS, R.drawable.ic_settings, "Settings"),
                ),
            )
        }
    }
}
