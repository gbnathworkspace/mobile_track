package com.mobiletrack.app.service

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.mobiletrack.app.data.local.dao.AppUsageDao
import com.mobiletrack.app.data.local.entity.AppUsageSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageDao: AppUsageDao
) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun syncTodayUsage() {
        val today = dateFormat.format(Date())
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = calendar.timeInMillis
        val endMs = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startMs, endMs
        )

        stats
            .filter { it.totalTimeInForeground > 0 }
            .forEach { stat ->
                val appName = runCatching {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(stat.packageName, 0)
                    ).toString()
                }.getOrDefault(stat.packageName)

                val minutes = (stat.totalTimeInForeground / 60_000).toInt()
                if (minutes > 0) {
                    appUsageDao.upsert(
                        AppUsageSession(
                            packageName = stat.packageName,
                            appName = appName,
                            date = today,
                            totalMinutes = minutes,
                            openCount = 1 // UsageStats doesn't give open count directly; refined via AccessibilityService
                        )
                    )
                }
            }
    }

    fun getCurrentForegroundApp(): String? {
        val endMs = System.currentTimeMillis()
        val startMs = endMs - 10_000
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startMs, endMs
        )
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }
}
