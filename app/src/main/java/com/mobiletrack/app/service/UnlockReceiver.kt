package com.mobiletrack.app.service

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mobiletrack.app.data.local.dao.UnlockDao
import com.mobiletrack.app.data.local.entity.UnlockEvent
import com.mobiletrack.app.data.preferences.UserPreferences
import com.mobiletrack.app.presentation.unlock.UnlockPromptActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class UnlockReceiver : BroadcastReceiver() {

    @Inject lateinit var unlockDao: UnlockDao
    @Inject lateinit var userPreferences: UserPreferences

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action=${intent.action}")
        if (intent.action != Intent.ACTION_USER_PRESENT && intent.action != Intent.ACTION_SCREEN_ON) return

        scope.launch {
            if (!shouldShowPrompt(context, intent.action)) {
                Log.d(TAG, "Skipping prompt for action=${intent.action}")
                return@launch
            }

            // Record unlock event
            unlockDao.insert(UnlockEvent())
            Log.d(TAG, "Recorded unlock event for action=${intent.action}")

            // Show unlock prompt if enabled
            val promptEnabled = userPreferences.unlockPromptEnabled.first()
            Log.d(TAG, "unlockPromptEnabled=$promptEnabled")
            if (promptEnabled) {
                val promptIntent = Intent(context, UnlockPromptActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    )
                }
                Log.d(TAG, "Starting UnlockPromptActivity")
                context.startActivity(promptIntent)
            }
        }
    }

    private suspend fun shouldShowPrompt(context: Context, action: String?): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (action == Intent.ACTION_SCREEN_ON) {
            // Give biometric/PIN unlock a moment to complete before checking lock state.
            delay(1200)
            val lockedAfterDelay = keyguardManager.isDeviceLocked
            Log.d(TAG, "SCREEN_ON check after delay: isDeviceLocked=$lockedAfterDelay")
            if (lockedAfterDelay) return false
        }

        val now = System.currentTimeMillis()
        synchronized(Companion) {
            if (now - lastPromptLaunchAt < PROMPT_DEBOUNCE_MS) {
                Log.d(TAG, "Debounced prompt launch")
                return false
            }
            lastPromptLaunchAt = now
        }
        return true
    }

    companion object {
        private const val TAG = "UnlockReceiver"
        private const val PROMPT_DEBOUNCE_MS = 3_000L
        private var lastPromptLaunchAt: Long = 0L
    }
}
