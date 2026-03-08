package com.mobiletrack.app.presentation.reports

import androidx.lifecycle.ViewModel
import com.mobiletrack.app.data.local.dao.AppUsageDao
import com.mobiletrack.app.data.local.dao.UnlockDao
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val appUsageDao: AppUsageDao,
    private val unlockDao: UnlockDao
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val sevenDaysAgoMs: Long
        get() = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L

    private val sevenDaysAgoDate: String
        get() = dateFormat.format(Date(sevenDaysAgoMs))

    val weeklyUsage = appUsageDao.getUsageFrom(sevenDaysAgoDate)

    val weeklyUnlocks = unlockDao.countSince(sevenDaysAgoMs)
}
