package com.mobiletrack.app.service

import android.app.*
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mobiletrack.app.R
import com.mobiletrack.app.data.local.dao.AppRuleDao
import com.mobiletrack.app.data.local.dao.AppUsageDao
import com.mobiletrack.app.data.local.dao.UnlockDao
import com.mobiletrack.app.data.local.entity.UnlockEvent
import com.mobiletrack.app.data.preferences.UserPreferences
import com.mobiletrack.app.presentation.MainActivity
import com.mobiletrack.app.presentation.unlock.UnlockPromptActivity
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
    private var lastPromptLaunchAt = 0L

    private val unlockBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "TrackingService receiver action=${intent.action}")
            if (intent.action != Intent.ACTION_USER_PRESENT && intent.action != Intent.ACTION_SCREEN_ON) return
            scope.launch {
                handleUnlockSignal(intent.action)
            }
        }
    }

    companion object {
        private const val TAG = "TrackingService"
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIF_ID = 1
        const val SYNC_INTERVAL_MS = 30_000L // sync every 30 seconds
        private const val PROMPT_DEBOUNCE_MS = 3_000L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        registerUnlockReceiver()
        startTracking()
    }

    private fun registerUnlockReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                this,
                unlockBroadcastReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
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

    private suspend fun handleUnlockSignal(action: String?) {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (action == Intent.ACTION_SCREEN_ON) {
            delay(1200)
            val lockedAfterDelay = keyguardManager.isDeviceLocked
            Log.d(TAG, "SCREEN_ON check after delay: isDeviceLocked=$lockedAfterDelay")
            if (lockedAfterDelay) return
        }

        val now = System.currentTimeMillis()
        if (now - lastPromptLaunchAt < PROMPT_DEBOUNCE_MS) {
            Log.d(TAG, "Debounced unlock prompt launch")
            return
        }
        lastPromptLaunchAt = now

        unlockDao.insert(UnlockEvent())
        val promptEnabled = userPreferences.unlockPromptEnabled.first()
        Log.d(TAG, "unlockPromptEnabled=$promptEnabled")
        if (!promptEnabled) return

        val promptIntent = Intent(this, UnlockPromptActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
        }
        Log.d(TAG, "Starting UnlockPromptActivity from TrackingService")
        startActivity(promptIntent)
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
        unregisterReceiver(unlockBroadcastReceiver)
        scope.cancel()
        super.onDestroy()
    }
}
