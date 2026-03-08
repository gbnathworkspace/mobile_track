package com.mobiletrack.app.presentation.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobiletrack.app.data.local.dao.UnlockDao
import com.mobiletrack.app.data.local.entity.UnlockEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnlockPromptViewModel @Inject constructor(
    private val unlockDao: UnlockDao
) : ViewModel() {

    fun recordPurpose(purpose: String) {
        viewModelScope.launch {
            // Update the most recent unlock event with the selected purpose
            unlockDao.insert(UnlockEvent(hadPurpose = true, purpose = purpose))
        }
    }

    fun recordNoPurpose() {
        viewModelScope.launch {
            unlockDao.insert(UnlockEvent(hadPurpose = false, purpose = null))
        }
    }
}
