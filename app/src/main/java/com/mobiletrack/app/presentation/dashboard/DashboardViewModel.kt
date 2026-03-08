package com.mobiletrack.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import com.mobiletrack.app.data.local.dao.AppUsageDao
import com.mobiletrack.app.data.local.dao.UnlockDao
import com.mobiletrack.app.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appUsageDao: AppUsageDao,
    private val unlockDao: UnlockDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val today: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private val startOfDayMs: Long
        get() = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    val totalMinutesToday = appUsageDao.getTotalMinutesForDate(today)

    val unlockCountToday = unlockDao.countSince(startOfDayMs)

    val topAppsToday = appUsageDao.getUsageForDate(today)

    val streakDays = userPreferences.streakDays
}
