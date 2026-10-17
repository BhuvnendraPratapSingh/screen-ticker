package com.socialwatch.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.socialwatch.app.ui.navigation.NavGraph
import com.socialwatch.app.ui.navigation.SWBottomNavBar
import com.socialwatch.app.ui.navigation.Screen
import com.socialwatch.app.ui.theme.SocialWatchTheme
import com.socialwatch.app.util.Constants
import com.socialwatch.app.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* No action needed — we just requested it */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            var darkMode by remember {
                mutableStateOf(prefs.getBoolean(Constants.KEY_DARK_MODE, false))
            }

            SocialWatchTheme(darkTheme = darkMode) {
                SocialWatchApp(
                    onDarkModeChange = { darkMode = it }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning from settings
        // The navController will pick this up via the composition
    }
}

@Composable
fun SocialWatchApp(
    onDarkModeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Determine start destination
    val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    val onboardingComplete = prefs.getBoolean(Constants.KEY_ONBOARDING_COMPLETE, false)
    var hasUsagePermission by remember {
        mutableStateOf(PermissionHelper.hasUsageStatsPermission(context))
    }

    // Re-check permission every time we resume (user might have granted it in Settings)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val granted = PermissionHelper.hasUsageStatsPermission(context)
            if (granted && !hasUsagePermission) {
                hasUsagePermission = true
                // Navigate to dashboard when permission is freshly granted
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            hasUsagePermission = granted
        }
    }

    val startDestination = when {
        !onboardingComplete -> Screen.Onboarding.route
        !hasUsagePermission -> Screen.Permission.route
        else -> Screen.Dashboard.route
    }

    // Determine if bottom bar should be visible
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.ManageApps.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                SWBottomNavBar(navController = navController)
            }
        }
    ) { padding ->
        NavGraph(
            navController = navController,
            startDestination = startDestination,
            padding = padding,
            onOpenUsageSettings = {
                context.startActivity(PermissionHelper.usageStatsSettingsIntent())
            },
            onDarkModeChange = onDarkModeChange,
            onOnboardingComplete = {
                val editor = prefs.edit()
                editor.putBoolean(Constants.KEY_ONBOARDING_COMPLETE, true)
                editor.apply()
            }
        )
    }
}
