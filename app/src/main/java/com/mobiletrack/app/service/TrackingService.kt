package com.mobiletrack.app.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mobiletrack.app.data.local.dao.AppRuleDao
import com.mobiletrack.app.data.local.dao.UnlockDao
import com.mobiletrack.app.data.preferences.UserPreferences
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
    @Inject lateinit var unlockDao: UnlockDao
    @Inject lateinit var userPreferences: UserPreferences

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Dynamic receiver for SCREEN_ON/SCREEN_OFF (cannot be received via manifest on API 26+)
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    scope.launch {
                        UnlockHandler.handleUnlock(context, intent.action, unlockDao, userPreferences)
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    ScreenState.lastScreenOffAt = System.currentTimeMillis()
                }
            }
        }
    }

    companion object {
        private const val TAG = "TrackingService"
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIF_ID = 1
        const val SYNC_INTERVAL_MS = 30_000L // sync every 30 seconds
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        registerScreenReceiver()
        UnlockHandler.observePreferences(userPreferences, scope)
        startTracking()
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                this,
                screenReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    private fun startTracking() {
        scope.launch {
            while (isActive) {
                try {
                    usageTracker.syncTodayUsage()
                    checkLimits()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Sync failed", e)
                }
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkLimits() {
        val today = dateFormat.format(Date())
        val limitedApps = appRuleDao.getLimitedApps().first()
        // Enforcement is handled by BlockerAccessibilityService — this just syncs data
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
        unregisterReceiver(screenReceiver)
        scope.cancel()
        super.onDestroy()
    }
}
