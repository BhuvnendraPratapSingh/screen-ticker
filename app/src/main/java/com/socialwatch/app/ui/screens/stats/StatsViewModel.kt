package com.socialwatch.app.ui.screens.stats

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.socialwatch.app.data.db.dao.TrackedAppDao
import com.socialwatch.app.data.repository.UsageStatsRepository
import com.socialwatch.app.ui.components.BarChartEntry
import com.socialwatch.app.ui.components.BarSegment
import com.socialwatch.app.ui.theme.ChartColors
import com.socialwatch.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

enum class TimeRange { DAY, WEEK, MONTH }

data class StatsState(
    val selectedRange: TimeRange = TimeRange.WEEK,
    val chartEntries: List<BarChartEntry> = emptyList(),
    val totalMs: Long = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    application: Application,
    private val usageStatsRepository: UsageStatsRepository,
    private val trackedAppDao: TrackedAppDao
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayLabelFormat = SimpleDateFormat("EEE", Locale.US)
    private val dayNumberFormat = SimpleDateFormat("d", Locale.US)

    init {
        loadData(TimeRange.WEEK)
        startAutoRefresh()
    }

    fun selectRange(range: TimeRange) {
        if (range == _state.value.selectedRange) return
        _state.value = _state.value.copy(selectedRange = range, isLoading = true)
        loadData(range)
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(3000) // Refresh every 3 seconds for real-time data
                loadData(_state.value.selectedRange)
            }
        }
    }

    private fun loadData(range: TimeRange) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    when (range) {
                        TimeRange.DAY -> loadDailyData()
                        TimeRange.WEEK -> loadRangeData(7, range)
                        TimeRange.MONTH -> loadRangeData(30, range)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun loadDailyData() {
        val trackedApps = trackedAppDao.getEnabledTrackedAppsList()
        val packageNames = trackedApps.map { it.packageName }
        val appNames = trackedApps.associate { it.packageName to it.appName }
        val appColors = buildColorMap(packageNames)

        // Get today's usage from system API
        val usageMap = usageStatsRepository.getTodayUsageForAllPackages(packageNames)
        val totalMs = usageMap.values.sum()

        // Create hourly buckets: 6 blocks of 4 hours each
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val blocksToShow = (currentHour / 4) + 1  // Only show passed time blocks + current

        val entries = (0..5).map { block ->
            val startHour = block * 4
            val endHour = startHour + 4
            val label = String.format("%02d:00", startHour)

            // Show proportional usage based on whether this block has passed
            val blockActive = currentHour >= startHour
            val segments = if (blockActive) {
                trackedApps.mapNotNull { app ->
                    val appTotal = usageMap[app.packageName] ?: 0L
                    // Distribute proportionally across active blocks
                    val blockShare = if (blocksToShow > 0) appTotal / blocksToShow else 0L
                    if (blockShare > 60_000) { // At least 1 minute to show
                        BarSegment(
                            durationMs = blockShare,
                            color = appColors[app.packageName] ?: ChartColors[0],
                            label = appNames[app.packageName] ?: app.packageName
                        )
                    } else null
                }
            } else {
                emptyList()
            }

            BarChartEntry(label = label, segments = segments)
        }

        _state.value = StatsState(
            selectedRange = TimeRange.DAY,
            chartEntries = entries,
            totalMs = totalMs,
            isLoading = false
        )
    }

    private suspend fun loadRangeData(days: Int, range: TimeRange) {
        val trackedApps = trackedAppDao.getEnabledTrackedAppsList()
        val packageNames = trackedApps.map { it.packageName }
        val appNames = trackedApps.associate { it.packageName to it.appName }
        val appColors = buildColorMap(packageNames)

        val today = Calendar.getInstance()
        val startCal = today.clone() as Calendar
        startCal.add(Calendar.DAY_OF_YEAR, -(days - 1))

        // Get daily breakdown from system API
        val dailyUsage = usageStatsRepository.getDailyUsageForRange(
            packageNames,
            startCal.timeInMillis,
            System.currentTimeMillis()
        )

        // Also get today's real-time data (system API daily range may lag)
        val todayDate = dateFormat.format(today.time)
        val todayUsage = usageStatsRepository.getTodayUsageForAllPackages(packageNames)

        var totalMs = 0L

        val entries = (0 until days).map { dayOffset ->
            val cal = startCal.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, dayOffset)
            val date = dateFormat.format(cal.time)
            val label = if (days <= 7) {
                dayLabelFormat.format(cal.time)
            } else {
                if (dayOffset % 5 == 0) dayNumberFormat.format(cal.time) else ""
            }

            // Use real-time data for today, historical data for past days
            val dayData = if (date == todayDate) {
                todayUsage
            } else {
                dailyUsage[date] ?: emptyMap()
            }

            val segments = dayData.mapNotNull { (pkg, ms) ->
                if (ms > 0 && pkg in packageNames) {
                    totalMs += ms
                    BarSegment(
                        durationMs = ms,
                        color = appColors[pkg] ?: ChartColors[0],
                        label = appNames[pkg] ?: pkg
                    )
                } else null
            }

            BarChartEntry(label = label, segments = segments)
        }

        _state.value = StatsState(
            selectedRange = range,
            chartEntries = entries,
            totalMs = totalMs,
            isLoading = false
        )
    }

    private fun buildColorMap(packageNames: List<String>): Map<String, Color> {
        return packageNames.mapIndexed { index, pkg ->
            val brandColor = Constants.APP_BRAND_COLORS[pkg]
            pkg to if (brandColor != null) Color(brandColor) else ChartColors[index % ChartColors.size]
        }.toMap()
    }
}
