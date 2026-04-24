package com.mobiletrack.app.presentation.unlock

import androidx.lifecycle.ViewModel
import com.mobiletrack.app.data.local.dao.UnlockDao
import com.mobiletrack.app.data.local.entity.UnlockEvent
import com.mobiletrack.app.data.preferences.UserPreferences
import com.mobiletrack.app.presentation.theme.UnlockTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class UnlockPromptViewModel @Inject constructor(
    private val unlockDao: UnlockDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val unlockTheme = userPreferences.unlockTheme.map { name ->
        when (name) {
            "DARK" -> UnlockTheme.DARK
            "GLASS" -> UnlockTheme.GLASS
            else -> UnlockTheme.LIGHT
        }
    }

    suspend fun recordPurpose(purpose: String) {
        unlockDao.insert(UnlockEvent(hadPurpose = true, purpose = purpose))
    }

    suspend fun recordNoPurpose() {
        unlockDao.insert(UnlockEvent(hadPurpose = false, purpose = null))
    }
}
