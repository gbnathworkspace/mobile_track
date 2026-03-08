package com.mobiletrack.app.presentation.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobiletrack.app.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val maxUnlocks = userPreferences.maxUnlocksPerDay
    val promptEnabled = userPreferences.unlockPromptEnabled

    fun setMaxUnlocks(value: Int) {
        viewModelScope.launch { userPreferences.setMaxUnlocksPerDay(value) }
    }

    fun setPromptEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setUnlockPromptEnabled(enabled) }
    }
}
