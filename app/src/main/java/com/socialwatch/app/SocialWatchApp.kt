package com.socialwatch.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SocialWatchApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // Channel for persistent monitoring notification
        val monitoringChannel = NotificationChannel(
            CHANNEL_MONITORING,
            getString(R.string.channel_monitoring),
            NotificationManager.IMPORTANCE_LOW  // Low = no sound, shows in bar
        ).apply {
            description = getString(R.string.channel_monitoring_desc)
            setShowBadge(false)
        }

        // Channel for time limit alert notifications
        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            getString(R.string.channel_alerts),
            NotificationManager.IMPORTANCE_HIGH  // High = heads-up notification
        ).apply {
            description = getString(R.string.channel_alerts_desc)
            enableVibration(true)
        }

        manager.createNotificationChannel(monitoringChannel)
        manager.createNotificationChannel(alertChannel)
    }

    companion object {
        const val CHANNEL_MONITORING = "monitoring_channel"
        const val CHANNEL_ALERTS = "alerts_channel"
    }
}
