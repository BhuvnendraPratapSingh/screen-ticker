package com.socialwatch.app.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository wrapping UsageStatsManager to query real device usage data.
 */
@Singleton
class UsageStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Get the package name of the app currently in the foreground.
     * Looks back up to 30 seconds to catch the most recent ACTIVITY_RESUMED event.
     */
    fun getCurrentForegroundApp(): String? {
        val endTime = System.currentTimeMillis()
        // Look back 30 seconds — some devices delay reporting events
        val startTime = endTime - 30_000

        return try {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            var lastForegroundPackage: String? = null
            var lastForegroundTime = 0L

            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                // ACTIVITY_RESUMED = app came to foreground
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    if (event.timeStamp >= lastForegroundTime) {
                        lastForegroundPackage = event.packageName
                        lastForegroundTime = event.timeStamp
                    }
                }
            }

            lastForegroundPackage
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get total foreground time today for a specific package (in milliseconds).
     * Uses the system UsageStats API — this is the most reliable source of truth.
     */
    fun getTodayUsageForPackage(packageName: String): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        return try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            stats
                .filter { it.packageName == packageName }
                .maxByOrNull { it.totalTimeInForeground }
                ?.totalTimeInForeground ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    /**
     * Get total foreground time for a package in a given date range.
     * Used for weekly/monthly charts.
     */
    fun getUsageForPackageInRange(packageName: String, startMs: Long, endMs: Long): Long {
        return try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startMs,
                endMs
            )

            stats
                .filter { it.packageName == packageName }
                .sumOf { it.totalTimeInForeground }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    /**
     * Get usage for ALL packages today, returned as a map of packageName → totalMs.
     */
    fun getTodayUsageForAllPackages(packageNames: List<String>): Map<String, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        return try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            val result = mutableMapOf<String, Long>()
            for (pkg in packageNames) {
                val total = stats
                    .filter { it.packageName == pkg }
                    .maxByOrNull { it.totalTimeInForeground }
                    ?.totalTimeInForeground ?: 0L
                result[pkg] = total
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            packageNames.associateWith { 0L }
        }
    }

    /**
     * Get daily usage breakdown for a date range.
     * Returns Map<date, Map<packageName, totalMs>>
     */
    fun getDailyUsageForRange(
        packageNames: List<String>,
        startMs: Long,
        endMs: Long
    ): Map<String, Map<String, Long>> {
        val result = mutableMapOf<String, MutableMap<String, Long>>()

        return try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startMs,
                endMs
            )

            for (stat in stats) {
                if (stat.packageName in packageNames && stat.totalTimeInForeground > 0) {
                    // Determine the date for this stat
                    val date = dateFormat.format(stat.firstTimeStamp)
                    val dateMap = result.getOrPut(date) { mutableMapOf() }
                    val current = dateMap[stat.packageName] ?: 0L
                    dateMap[stat.packageName] = maxOf(current, stat.totalTimeInForeground)
                }
            }

            result
        } catch (e: Exception) {
            e.printStackTrace()
            result
        }
    }

    fun getTodayDate(): String {
        return dateFormat.format(System.currentTimeMillis())
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(timestamp)
    }
}
