package com.socialwatch.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.socialwatch.app.service.ServiceController
import com.socialwatch.app.util.Constants

/**
 * Starts the monitoring service automatically when the device boots,
 * if the user has enabled the "Start on boot" setting.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val startOnBoot = prefs.getBoolean(Constants.KEY_START_ON_BOOT, true)
            val monitoringEnabled = prefs.getBoolean(Constants.KEY_MONITORING_ENABLED, false)

            if (startOnBoot && monitoringEnabled) {
                ServiceController.startMonitoring(context)
            }
        }
    }
}
