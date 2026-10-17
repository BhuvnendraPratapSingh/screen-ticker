package com.socialwatch.app.ui.screens.manageapps

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.socialwatch.app.data.db.dao.TrackedAppDao
import com.socialwatch.app.data.db.entity.TrackedApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)

@HiltViewModel
class ManageAppsViewModel @Inject constructor(
    private val application: Application,
    private val trackedAppDao: TrackedAppDao
) : AndroidViewModel(application) {

    val trackedApps = trackedAppDao.getAllTrackedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()

    fun loadInstalledApps() {
        viewModelScope.launch {
            val pm = application.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    // Exclude system apps and our own app
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                            app.packageName != application.packageName
                }
                .map { app ->
                    InstalledAppInfo(
                        packageName = app.packageName,
                        appName = pm.getApplicationLabel(app).toString(),
                        icon = try { pm.getApplicationIcon(app) } catch (_: Exception) { null }
                    )
                }
                .sortedBy { it.appName.lowercase() }

            _installedApps.value = apps
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun showAddAppSheet() {
        loadInstalledApps()
        _showAddSheet.value = true
    }

    fun hideAddAppSheet() {
        _showAddSheet.value = false
        _searchQuery.value = ""
    }

    fun addApp(packageName: String, appName: String) {
        viewModelScope.launch {
            val existing = trackedAppDao.getTrackedApp(packageName)
            if (existing == null) {
                trackedAppDao.insert(
                    TrackedApp(
                        packageName = packageName,
                        appName = appName,
                        dailyLimitMinutes = 0,
                        isEnabled = true
                    )
                )
            }
            hideAddAppSheet()
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            trackedAppDao.deleteByPackage(packageName)
        }
    }

    fun toggleApp(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            trackedAppDao.setEnabled(packageName, enabled)
        }
    }

    fun setTimeLimit(packageName: String, limitMinutes: Int) {
        viewModelScope.launch {
            trackedAppDao.setDailyLimit(packageName, limitMinutes)
        }
    }
}
