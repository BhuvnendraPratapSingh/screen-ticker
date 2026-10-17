package com.socialwatch.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Permission : Screen("permission")
    data object Dashboard : Screen("dashboard")
    data object ManageApps : Screen("manage_apps")
    data object Settings : Screen("settings")
    data object Statistics : Screen("statistics")
}

/**
 * Bottom navigation tab items.
 */
enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    Dashboard(Screen.Dashboard.route, Icons.Rounded.Home, "Dashboard"),
    ManageApps(Screen.ManageApps.route, Icons.Rounded.Apps, "Apps"),
    Settings(Screen.Settings.route, Icons.Rounded.Settings, "Settings")
}
