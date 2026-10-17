package com.socialwatch.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── iOS-Inspired Color Palette ──────────────────────────────────────────────

// Primary accent colors
val IOSBlue = Color(0xFF007AFF)
val IOSPurple = Color(0xFF5856D6)
val IOSRed = Color(0xFFFF3B30)
val IOSGreen = Color(0xFF34C759)
val IOSOrange = Color(0xFFFF9500)
val IOSYellow = Color(0xFFFFCC00)
val IOSTeal = Color(0xFF5AC8FA)
val IOSPink = Color(0xFFFF2D55)

// Light theme
val LightBackground = Color(0xFFF2F2F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE5E5EA)
val LightOnBackground = Color(0xFF000000)
val LightOnSurface = Color(0xFF000000)
val LightOnSurfaceVariant = Color(0xFF8E8E93)
val LightOutline = Color(0xFFC6C6C8)
val LightSeparator = Color(0x33000000)

// Dark theme
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF1C1C1E)
val DarkSurfaceVariant = Color(0xFF2C2C2E)
val DarkOnBackground = Color(0xFFFFFFFF)
val DarkOnSurface = Color(0xFFFFFFFF)
val DarkOnSurfaceVariant = Color(0xFF8E8E93)
val DarkOutline = Color(0xFF38383A)
val DarkSeparator = Color(0x33FFFFFF)

// Dark mode accent variants
val DarkBlue = Color(0xFF0A84FF)
val DarkPurple = Color(0xFF5E5CE6)
val DarkRed = Color(0xFFFF453A)
val DarkGreen = Color(0xFF30D158)
val DarkOrange = Color(0xFFFF9F0A)

// Social media brand colors (used for charts and per-app theming)
val InstagramColor = Color(0xFFE1306C)
val FacebookColor = Color(0xFF1877F2)
val TwitterColor = Color(0xFF1DA1F2)
val RedditColor = Color(0xFFFF4500)
val TikTokColor = Color(0xFF010101)
val YouTubeColor = Color(0xFFFF0000)
val SnapchatColor = Color(0xFFFFFC00)
val LinkedInColor = Color(0xFF0A66C2)
val DefaultAppColor = Color(0xFF8E8E93)

// Chart color palette (cycled for custom apps)
val ChartColors = listOf(
    InstagramColor,
    FacebookColor,
    TwitterColor,
    RedditColor,
    IOSPurple,
    IOSOrange,
    IOSTeal,
    IOSPink,
    IOSGreen,
    IOSYellow
)
