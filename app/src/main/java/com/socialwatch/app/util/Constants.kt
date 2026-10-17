package com.socialwatch.app.util

/**
 * App-wide constants.
 */
object Constants {

    // Monitoring
    const val POLL_INTERVAL_MS = 1000L          // Check foreground app every 1 second
    const val NOTIFICATION_ID_MONITORING = 1001
    const val NOTIFICATION_ID_ALERT = 2001

    // DataStore keys
    const val PREFS_NAME = "socialwatch_prefs"
    const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    const val KEY_START_ON_BOOT = "start_on_boot"
    const val KEY_DARK_MODE = "dark_mode"
    const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    const val KEY_SHOW_NOTIFICATION_TIMER = "show_notification_timer"

    // Default tracked apps
    val DEFAULT_TRACKED_APPS = mapOf(
        "com.instagram.android" to "Instagram",
        "com.facebook.katana" to "Facebook",
        "com.twitter.android" to "Twitter / X",
        "com.reddit.frontpage" to "Reddit"
    )

    // Package name → brand color mapping
    val APP_BRAND_COLORS = mapOf(
        "com.instagram.android" to 0xFFE1306C,
        "com.facebook.katana" to 0xFF1877F2,
        "com.twitter.android" to 0xFF1DA1F2,
        "com.reddit.frontpage" to 0xFFFF4500,
        "com.zhiliaoapp.musically" to 0xFF010101,   // TikTok
        "com.google.android.youtube" to 0xFFFF0000,  // YouTube
        "com.snapchat.android" to 0xFFFFFC00,         // Snapchat
        "com.linkedin.android" to 0xFF0A66C2          // LinkedIn
    )
}
