package com.mobiletrack.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        scope.launch {
            // Record unlock event
            unlockDao.insert(UnlockEvent())

            // Show unlock prompt if enabled
            val promptEnabled = userPreferences.unlockPromptEnabled.first()
            if (promptEnabled) {
                val promptIntent = Intent(context, UnlockPromptActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(promptIntent)
            }
        }
    }
}
