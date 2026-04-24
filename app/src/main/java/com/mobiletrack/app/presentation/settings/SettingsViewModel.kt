package com.mobiletrack.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobiletrack.app.data.preferences.UserPreferences
import com.mobiletrack.app.presentation.theme.UnlockTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val unlockTheme = userPreferences.unlockTheme
        .map { name ->
            when (name) {
                "DARK" -> UnlockTheme.DARK
                "GLASS" -> UnlockTheme.GLASS
                else -> UnlockTheme.LIGHT
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UnlockTheme.LIGHT)

    fun setUnlockTheme(theme: UnlockTheme) {
        viewModelScope.launch {
            userPreferences.setUnlockTheme(theme.name)
        }
    }
}
