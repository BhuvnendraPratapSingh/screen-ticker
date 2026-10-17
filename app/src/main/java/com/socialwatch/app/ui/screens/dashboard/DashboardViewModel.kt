package com.socialwatch.app.ui.screens.dashboard

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.socialwatch.app.data.db.dao.TrackedAppDao
import com.socialwatch.app.data.repository.UsageStatsRepository
import com.socialwatch.app.service.ServiceController
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
import javax.inject.Inject

data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val durationMs: Long,
    val limitMinutes: Int,
    val color: Color,
    val icon: Drawable? = null,
    val isLimitExceeded: Boolean = false
)

data class DashboardState(
    val totalTodayMs: Long = 0,
    val appUsages: List<AppUsageItem> = emptyList(),
    val isMonitoring: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val application: Application,
    private val trackedAppDao: TrackedAppDao,
    private val usageStatsRepository: UsageStatsRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        checkMonitoringStatus()
        startPeriodicRefresh()
    }

    private fun checkMonitoringStatus() {
        val isRunning = ServiceController.isServiceRunning(application)
        _state.value = _state.value.copy(isMonitoring = isRunning)
    }

    fun toggleMonitoring() {
        val currentState = _state.value.isMonitoring
        if (currentState) {
            ServiceController.stopMonitoring(application)
        } else {
            ServiceController.startMonitoring(application)
        }
        _state.value = _state.value.copy(isMonitoring = !currentState)
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (isActive) {
                refreshData()
                delay(2000) // Refresh every 2 seconds for real-time data
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                val trackedApps = trackedAppDao.getEnabledTrackedAppsList()
                val pm = application.packageManager

                // Use the system UsageStats API directly — most reliable source
                val packageNames = trackedApps.map { it.packageName }
                val usageMap = withContext(Dispatchers.IO) {
                    usageStatsRepository.getTodayUsageForAllPackages(packageNames)
                }

                val usages = trackedApps.mapIndexed { index, app ->
                    val totalMs = usageMap[app.packageName] ?: 0L
                    val brandColor = Constants.APP_BRAND_COLORS[app.packageName]
                    val color = if (brandColor != null) Color(brandColor) else {
                        ChartColors[index % ChartColors.size]
                    }

                    val icon = try {
                        pm.getApplicationIcon(app.packageName)
                    } catch (_: PackageManager.NameNotFoundException) {
                        null
                    }

                    val isExceeded = app.dailyLimitMinutes > 0 &&
                            totalMs >= app.dailyLimitMinutes * 60_000L

                    AppUsageItem(
                        packageName = app.packageName,
                        appName = app.appName,
                        durationMs = totalMs,
                        limitMinutes = app.dailyLimitMinutes,
                        color = color,
                        icon = icon,
                        isLimitExceeded = isExceeded
                    )
                }.sortedByDescending { it.durationMs }

                val totalToday = usages.sumOf { it.durationMs }

                _state.value = _state.value.copy(
                    totalTodayMs = totalToday,
                    appUsages = usages,
                    isLoading = false,
                    isMonitoring = ServiceController.isServiceRunning(application)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}
