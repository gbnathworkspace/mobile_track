package com.mobiletrack.app.service

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mobiletrack.app.data.local.dao.UnlockDao
import com.mobiletrack.app.data.local.entity.UnlockEvent
import com.mobiletrack.app.data.preferences.UserPreferences
import com.mobiletrack.app.presentation.unlock.UnlockPromptActivity
import com.mobiletrack.app.presentation.unlock.WakeUpPromptActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Centralized unlock handling shared by TrackingService (SCREEN_ON),
 * UnlockReceiver (USER_PRESENT), and BlockerAccessibilityService (fallback).
 * Single debounce prevents duplicate prompts when multiple triggers fire.
 */
object UnlockHandler {

    private const val TAG = "UnlockHandler"
    private const val PROMPT_DEBOUNCE_MS = 3_000L
    private const val SCREEN_ON_UNLOCK_POLL_INTERVAL_MS = 50L
    private const val SCREEN_ON_UNLOCK_MAX_WAIT_MS = 400L

    // Wake-up detection thresholds
    private const val MIN_IDLE_FOR_WAKEUP_MS = 2 * 60 * 60 * 1000L // 2 hours
    private const val WAKEUP_HOUR_START = 4   // 4 AM
    private const val WAKEUP_HOUR_END = 10    // 10 AM

    @Volatile
    private var lastPromptLaunchAt: Long = 0L

    // Cached preference to avoid DataStore latency on every unlock
    @Volatile
    private var cachedPromptEnabled: Boolean = true

    @Volatile
    private var prefCacheInitialized: Boolean = false

    /** Call once from TrackingService to warm the cache and keep it up to date. */
    fun observePreferences(userPreferences: UserPreferences, scope: kotlinx.coroutines.CoroutineScope) {
        scope.launch {
            userPreferences.unlockPromptEnabled.collect { enabled ->
                cachedPromptEnabled = enabled
                prefCacheInitialized = true
            }
        }
    }

    suspend fun handleUnlock(
        context: Context,
        action: String?,
        unlockDao: UnlockDao,
        userPreferences: UserPreferences
    ) {
        val keyguardManager =
            context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        // For SCREEN_ON: poll briefly for keyguard to clear
        if (action == Intent.ACTION_SCREEN_ON) {
            var unlocked = false
            repeat((SCREEN_ON_UNLOCK_MAX_WAIT_MS / SCREEN_ON_UNLOCK_POLL_INTERVAL_MS).toInt()) {
                delay(SCREEN_ON_UNLOCK_POLL_INTERVAL_MS)
                if (!keyguardManager.isDeviceLocked) {
                    unlocked = true
                    return@repeat
                }
            }
            if (!unlocked) {
                Log.d(TAG, "Device still locked after polling, skipping (action=$action)")
                return
            }
        }

        // Debounce across all trigger sources
        val now = System.currentTimeMillis()
        synchronized(this) {
            if (now - lastPromptLaunchAt < PROMPT_DEBOUNCE_MS) {
                Log.d(TAG, "Debounced (action=$action)")
                return
            }
            lastPromptLaunchAt = now
        }

        // Use cached preference (fast path) or fall back to DataStore read
        val promptEnabled = if (prefCacheInitialized) cachedPromptEnabled
            else userPreferences.unlockPromptEnabled.first()

        if (!promptEnabled) {
            unlockDao.insert(UnlockEvent())
            Log.d(TAG, "Prompt disabled, skipping (action=$action)")
            return
        }

        // Launch prompt immediately, check wake-up in parallel
        // This eliminates the DB query delay before the prompt appears
        val isWakeUp = isWakeUpUnlock(now, unlockDao)

        val targetClass = if (isWakeUp) {
            Log.d(TAG, "Wake-up unlock detected")
            WakeUpPromptActivity::class.java
        } else {
            Log.d(TAG, "Launching UnlockPromptActivity (action=$action)")
            UnlockPromptActivity::class.java
        }

        val promptIntent = Intent(context, targetClass).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }

        // Launch with zero animation for instant appearance
        val options = ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()

        withContext(Dispatchers.Main.immediate) {
            context.startActivity(promptIntent, options)
        }

        unlockDao.insert(UnlockEvent())
        Log.d(TAG, "Recorded unlock event (action=$action)")
    }

    private suspend fun isWakeUpUnlock(now: Long, unlockDao: UnlockDao): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour < WAKEUP_HOUR_START || hour >= WAKEUP_HOUR_END) return false

        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val unlocksToday = unlockDao.countSince(startOfDay).first()
        if (unlocksToday > 0) return false

        val lastOff = ScreenState.lastScreenOffAt
        if (lastOff > 0L) {
            val idleDuration = now - lastOff
            if (idleDuration < MIN_IDLE_FOR_WAKEUP_MS) return false
        }

        return true
    }
}
