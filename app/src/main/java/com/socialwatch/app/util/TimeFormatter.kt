package com.socialwatch.app.util

/**
 * Utility for formatting durations into human-readable strings.
 */
object TimeFormatter {

    /**
     * Formats milliseconds into a readable duration string.
     * Examples: "2h 15m", "45m 12s", "30s"
     */
    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "0s"

        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Formats milliseconds into a short compact string for notifications.
     * Examples: "2h 15m", "45m", "< 1m"
     */
    fun formatShort(ms: Long): String {
        if (ms <= 0) return "0m"

        val totalMinutes = ms / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    /**
     * Formats milliseconds into hours as a decimal for chart labels.
     * Example: 5400000 → "1.5"
     */
    fun formatHoursDecimal(ms: Long): String {
        val hours = ms / 3_600_000.0
        return if (hours >= 1.0) {
            String.format("%.1fh", hours)
        } else {
            val minutes = ms / 60_000
            "${minutes}m"
        }
    }
}
