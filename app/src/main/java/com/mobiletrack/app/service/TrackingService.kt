package com.mobiletrack.app.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mobiletrack.app.R
import com.mobiletrack.app.data.local.dao.AppRuleDao
import com.mobiletrack.app.data.local.dao.AppUsageDao
import com.mobiletrack.app.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : Service() {

    @Inject lateinit var usageTracker: UsageTracker
    @Inject lateinit var appRuleDao: AppRuleDao

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIF_ID = 1
        const val SYNC_INTERVAL_MS = 60_000L // sync every minute
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        startTracking()
    }

    private fun startTracking() {
        scope.launch {
            while (isActive) {
                usageTracker.syncTodayUsage()
                checkLimits()
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkLimits() {
        val today = dateFormat.format(Date())
        val limitedApps = appRuleDao.getLimitedApps().first()
        // Enforcement is handled by BlockerAccessibilityService — this just syncs data
        // Future: broadcast an intent to notify the accessibility service of breached limits
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "MobileTrack Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Runs in background to track screen time"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MobileTrack active")
            .setContentText("Monitoring your screen time")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
