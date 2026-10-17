package com.socialwatch.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.socialwatch.app.ui.screens.dashboard.DashboardScreen
import com.socialwatch.app.ui.screens.manageapps.ManageAppsScreen
import com.socialwatch.app.ui.screens.onboarding.OnboardingScreen
import com.socialwatch.app.ui.screens.permission.PermissionScreen
import com.socialwatch.app.ui.screens.settings.SettingsScreen
import com.socialwatch.app.ui.screens.stats.StatsScreen

/**
 * Main navigation graph for the app.
 *
 * @param startDestination The initial route, determined by permission/onboarding state.
 * @param onOpenUsageSettings Callback to open system Usage Access settings.
 * @param onDarkModeChange Callback when dark mode toggle changes.
 * @param onOnboardingComplete Callback when onboarding is finished.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    padding: PaddingValues,
    onOpenUsageSettings: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onOnboardingComplete: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(padding),
        enterTransition = {
            fadeIn(animationSpec = tween(200))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200))
        }
    ) {
        // Onboarding flow
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    onOnboardingComplete()
                    navController.navigate(Screen.Permission.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Permission request screen
        composable(Screen.Permission.route) {
            PermissionScreen(
                onOpenSettings = onOpenUsageSettings
            )
        }

        // Main tabs
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToStats = {
                    navController.navigate(Screen.Statistics.route)
                }
            )
        }

        composable(Screen.ManageApps.route) {
            ManageAppsScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onDarkModeChange = onDarkModeChange
            )
        }

        // Statistics (pushed from Dashboard)
        composable(
            route = Screen.Statistics.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            StatsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
