package com.mobiletrack.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val MAX_UNLOCKS_PER_DAY = intPreferencesKey("max_unlocks_per_day")
        val UNLOCK_PROMPT_ENABLED = booleanPreferencesKey("unlock_prompt_enabled")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val BEHAVIOR_SCORE_TODAY = intPreferencesKey("behavior_score_today")
        val STREAK_DAYS = intPreferencesKey("streak_days")
    }

    val maxUnlocksPerDay: Flow<Int> = context.dataStore.data.map {
        it[Keys.MAX_UNLOCKS_PER_DAY] ?: 50
    }

    val unlockPromptEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.UNLOCK_PROMPT_ENABLED] ?: true
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.ONBOARDING_COMPLETE] ?: false
    }

    val streakDays: Flow<Int> = context.dataStore.data.map {
        it[Keys.STREAK_DAYS] ?: 0
    }

    suspend fun setMaxUnlocksPerDay(value: Int) {
        context.dataStore.edit { it[Keys.MAX_UNLOCKS_PER_DAY] = value }
    }

    suspend fun setUnlockPromptEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.UNLOCK_PROMPT_ENABLED] = enabled }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setStreakDays(days: Int) {
        context.dataStore.edit { it[Keys.STREAK_DAYS] = days }
    }
}
