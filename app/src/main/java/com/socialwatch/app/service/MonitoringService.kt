package com.socialwatch.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.socialwatch.app.MainActivity
import com.socialwatch.app.R
import com.socialwatch.app.SocialWatchApp
import com.socialwatch.app.data.db.dao.DailySummaryDao
import com.socialwatch.app.data.db.dao.TrackedAppDao
import com.socialwatch.app.data.db.dao.UsageSessionDao
import com.socialwatch.app.data.db.entity.DailySummary
import com.socialwatch.app.data.db.entity.UsageSession
import com.socialwatch.app.data.repository.UsageStatsRepository
import com.socialwatch.app.util.Constants
import com.socialwatch.app.util.TimeFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : Service() {

    @Inject lateinit var trackedAppDao: TrackedAppDao
    @Inject lateinit var usageSessionDao: UsageSessionDao
    @Inject lateinit var dailySummaryDao: DailySummaryDao
    @Inject lateinit var usageStatsRepository: UsageStatsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitoringJob: Job? = null

    // State tracking
    private var currentTrackedPackage: String? = null
    private var currentSessionId: Long? = null
    private var currentSessionStart: Long = 0
    private var currentDate: String = ""
    private val alertedAppsToday = mutableSetOf<String>()
    private var overlayView: android.view.View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        currentDate = usageStatsRepository.getTodayDate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(Constants.NOTIFICATION_ID_MONITORING, createIdleNotification())
        startMonitoringLoop()

        // Save enabled state
        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(Constants.KEY_MONITORING_ENABLED, true)
            .apply()

        return START_STICKY
    }

    override fun onDestroy() {
        monitoringJob?.cancel()

        // Close any open session BEFORE cancelling the scope
        try {
            runBlocking {
                closeCurrentSession()
            }
        } catch (_: Exception) {}

        serviceScope.cancel()
        removeOverlayIfShowing()

        getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(Constants.KEY_MONITORING_ENABLED, false)
            .apply()

        super.onDestroy()
    }

    private fun startMonitoringLoop() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                try {
                    pollForegroundApp()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(Constants.POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollForegroundApp() {
        // Check for date change (midnight reset)
        val today = usageStatsRepository.getTodayDate()
        if (today != currentDate) {
            currentDate = today
            alertedAppsToday.clear()
            closeCurrentSession()
        }

        val foregroundApp = usageStatsRepository.getCurrentForegroundApp()
        val trackedPackages = trackedAppDao.getEnabledPackageNames()

        if (foregroundApp != null && foregroundApp in trackedPackages) {
            // Tracked app is in foreground
            if (foregroundApp != currentTrackedPackage) {
                // Switched to a different tracked app
                closeCurrentSession()
                startNewSession(foregroundApp)
            } else if (currentSessionId != null) {
                // Same app still in foreground — update the running session duration
                updateActiveSession()
            }

            // Get usage from system API (most reliable)
            val totalFromSystem = usageStatsRepository.getTodayUsageForPackage(foregroundApp)

            // Also add current active session time for real-time accuracy
            val activeSessionDuration = if (currentSessionStart > 0) {
                System.currentTimeMillis() - currentSessionStart
            } else 0L

            // Use the higher of system API or our tracking (system may lag slightly)
            val totalMs = maxOf(totalFromSystem, activeSessionDuration)

            val appInfo = trackedAppDao.getTrackedApp(foregroundApp)
            val appName = appInfo?.appName ?: foregroundApp
            val timeStr = TimeFormatter.formatShort(totalMs)

            // Check if user wants notification timer
            val showTimer = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                .getBoolean(Constants.KEY_SHOW_NOTIFICATION_TIMER, true)

            if (showTimer) {
                updateNotification(
                    getString(R.string.notification_monitoring_active, appName, timeStr)
                )
            } else {
                updateNotification(getString(R.string.notification_monitoring_idle))
            }

            // Check time limit
            if (appInfo != null && appInfo.dailyLimitMinutes > 0) {
                val limitMs = appInfo.dailyLimitMinutes * 60_000L
                if (totalMs >= limitMs && foregroundApp !in alertedAppsToday) {
                    alertedAppsToday.add(foregroundApp)
                    sendTimeLimitAlert(appName, timeStr)
                    showTimeLimitOverlay(appName, timeStr)
                }
            }

            // Update daily summary with system data
            updateDailySummary(foregroundApp, totalMs)

        } else {
            // No tracked app in foreground
            if (currentTrackedPackage != null) {
                closeCurrentSession()
            }
            updateNotification(getString(R.string.notification_monitoring_idle))
        }
    }

    private suspend fun startNewSession(packageName: String) {
        currentTrackedPackage = packageName
        currentSessionStart = System.currentTimeMillis()

        val session = UsageSession(
            packageName = packageName,
            startTime = currentSessionStart,
            endTime = 0,
            durationMs = 0,
            date = currentDate
        )
        currentSessionId = usageSessionDao.insert(session)
    }

    private suspend fun updateActiveSession() {
        val sessionId = currentSessionId ?: return
        val session = usageSessionDao.getById(sessionId) ?: return
        if (session.endTime == 0L) {
            val now = System.currentTimeMillis()
            usageSessionDao.update(
                session.copy(
                    durationMs = now - session.startTime
                )
            )
        }
    }

    private suspend fun closeCurrentSession() {
        val pkg = currentTrackedPackage ?: return
        val sessionId = currentSessionId ?: return
        val endTime = System.currentTimeMillis()

        try {
            val session = usageSessionDao.getById(sessionId)
            if (session != null && session.endTime == 0L) {
                usageSessionDao.update(
                    session.copy(
                        endTime = endTime,
                        durationMs = endTime - session.startTime
                    )
                )
            }
        } catch (_: Exception) {}

        currentTrackedPackage = null
        currentSessionId = null
        currentSessionStart = 0
    }

    private suspend fun updateDailySummary(packageName: String, totalMs: Long) {
        try {
            val exists = dailySummaryDao.exists(packageName, currentDate)
            if (exists > 0) {
                dailySummaryDao.updateTotal(packageName, currentDate, totalMs)
            } else {
                dailySummaryDao.insert(
                    DailySummary(
                        packageName = packageName,
                        date = currentDate,
                        totalDurationMs = totalMs
                    )
                )
            }
        } catch (_: Exception) {}
    }

    // ── Notifications ────────────────────────────────────────────────────────

    private fun createIdleNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SocialWatchApp.CHANNEL_MONITORING)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_monitoring_idle))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, SocialWatchApp.CHANNEL_MONITORING)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(Constants.NOTIFICATION_ID_MONITORING, notification)
        } catch (_: SecurityException) { }
    }

    private fun sendTimeLimitAlert(appName: String, timeSpent: String) {
        val notification = NotificationCompat.Builder(this, SocialWatchApp.CHANNEL_ALERTS)
            .setContentTitle(getString(R.string.notification_limit_title))
            .setContentText(getString(R.string.notification_limit_text, timeSpent, appName))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(this)
                .notify(Constants.NOTIFICATION_ID_ALERT, notification)
        } catch (_: SecurityException) { }
    }

    // ── Overlay Warning ──────────────────────────────────────────────────────

    private fun showTimeLimitOverlay(appName: String, timeSpent: String) {
        if (!android.provider.Settings.canDrawOverlays(this)) return

        serviceScope.launch(Dispatchers.Main) {
            removeOverlayIfShowing()

            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val overlayLayout = createOverlayView(appName, timeSpent, windowManager)
            overlayView = overlayLayout

            try {
                windowManager.addView(overlayLayout, params)
            } catch (_: Exception) { }
        }
    }

    @Suppress("SetTextI18n")
    private fun createOverlayView(
        appName: String,
        timeSpent: String,
        windowManager: WindowManager
    ): android.view.View {
        val density = resources.displayMetrics.density

        val root = android.widget.FrameLayout(this).apply {
            setBackgroundColor(0x99000000.toInt())
            isClickable = true
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val cardWidth = (300 * density).toInt()
            val pad = (28 * density).toInt()
            setPadding(pad, pad, pad, pad)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = 20 * density
            }
            layoutParams = android.widget.FrameLayout.LayoutParams(
                cardWidth,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }

        val title = TextView(this).apply {
            text = getString(R.string.time_limit_reached)
            textSize = 22f
            setTextColor(0xFF000000.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * density).toInt() }
        }

        val message = TextView(this).apply {
            text = getString(R.string.time_limit_message, timeSpent, appName)
            textSize = 16f
            setTextColor(0xFF8E8E93.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (24 * density).toInt() }
        }

        val dismissBtn = Button(this).apply {
            text = getString(R.string.dismiss)
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            isAllCaps = false
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFF007AFF.toInt())
                cornerRadius = 25 * density
            }
            setPadding((40 * density).toInt(), (12 * density).toInt(), (40 * density).toInt(), (12 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (50 * density).toInt()
            )
            setOnClickListener {
                try {
                    windowManager.removeView(root)
                    overlayView = null
                } catch (_: Exception) {}
            }
        }

        card.addView(title)
        card.addView(message)
        card.addView(dismissBtn)
        root.addView(card)
        return root
    }

    private fun removeOverlayIfShowing() {
        overlayView?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }
    }
}
