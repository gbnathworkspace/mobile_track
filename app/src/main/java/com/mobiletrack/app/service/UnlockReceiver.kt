package com.mobiletrack.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mobiletrack.app.data.local.dao.UnlockDao
import com.mobiletrack.app.data.preferences.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Manifest-registered receiver for ACTION_USER_PRESENT (device unlock).
 * Delegates to [UnlockHandler] for debouncing, recording, and prompt launching.
 *
 * Note: ACTION_SCREEN_ON cannot be received via manifest on API 26+,
 * so it is handled by a dynamic receiver in [TrackingService].
 */
@AndroidEntryPoint
class UnlockReceiver : BroadcastReceiver() {

    @Inject lateinit var unlockDao: UnlockDao
    @Inject lateinit var userPreferences: UserPreferences

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return
        Log.d(TAG, "USER_PRESENT received")

        val pendingResult = goAsync()
        scope.launch {
            try {
                UnlockHandler.handleUnlock(context, intent.action, unlockDao, userPreferences)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "UnlockReceiver"
    }
}
