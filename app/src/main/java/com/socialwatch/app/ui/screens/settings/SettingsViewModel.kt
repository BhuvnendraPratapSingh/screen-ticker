package com.socialwatch.app.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.socialwatch.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val startOnBoot: Boolean = true,
    val darkMode: Boolean = false,
    val onboardingComplete: Boolean = true,
    val showNotificationTimer: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        SettingsState(
            startOnBoot = prefs.getBoolean(Constants.KEY_START_ON_BOOT, true),
            darkMode = prefs.getBoolean(Constants.KEY_DARK_MODE, false),
            onboardingComplete = prefs.getBoolean(Constants.KEY_ONBOARDING_COMPLETE, false),
            showNotificationTimer = prefs.getBoolean(Constants.KEY_SHOW_NOTIFICATION_TIMER, true)
        )
    )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun setStartOnBoot(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.KEY_START_ON_BOOT, enabled).apply()
        _state.value = _state.value.copy(startOnBoot = enabled)
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.KEY_DARK_MODE, enabled).apply()
        _state.value = _state.value.copy(darkMode = enabled)
    }

    fun setShowNotificationTimer(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.KEY_SHOW_NOTIFICATION_TIMER, enabled).apply()
        _state.value = _state.value.copy(showNotificationTimer = enabled)
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean(Constants.KEY_ONBOARDING_COMPLETE, true).apply()
        _state.value = _state.value.copy(onboardingComplete = true)
    }

    fun isOnboardingComplete(): Boolean {
        return prefs.getBoolean(Constants.KEY_ONBOARDING_COMPLETE, false)
    }
}
