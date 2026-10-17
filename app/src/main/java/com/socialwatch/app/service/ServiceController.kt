package com.socialwatch.app.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Utility to start and stop the MonitoringService.
 */
object ServiceController {

    fun startMonitoring(context: Context) {
        val intent = Intent(context, MonitoringService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopMonitoring(context: Context) {
        val intent = Intent(context, MonitoringService::class.java)
        context.stopService(intent)
    }

    fun isServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (MonitoringService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
