package com.mobiletrack.app.presentation

import androidx.lifecycle.ViewModel
import com.mobiletrack.app.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    val onboardingComplete = userPreferences.onboardingComplete
}
