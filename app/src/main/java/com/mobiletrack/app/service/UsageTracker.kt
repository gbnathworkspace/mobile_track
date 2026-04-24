package com.mobiletrack.app.service

import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
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

        val usageByPackage = aggregateForegroundUsage(startMs, endMs)

        usageByPackage
            .filterValues { it > 0L }
            .filterKeys { it != context.packageName }
            .forEach { (packageName, foregroundMs) ->
                val appName = runCatching {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(packageName, 0)
                    ).toString()
                }.getOrDefault(packageName)

                val minutes = (foregroundMs / 60_000).toInt()
                if (minutes > 0) {
                    val now = System.currentTimeMillis()
                    // Insert a new row only if none exists for today (openCount starts at 0,
                    // AccessibilityService increments it on each window-focus event).
                    // If the row already exists, IGNORE leaves openCount untouched.
                    appUsageDao.insertIfAbsent(
                        AppUsageSession(
                            packageName = packageName,
                            appName = appName,
                            date = today,
                            totalMinutes = minutes,
                            openCount = 0,
                            updatedAt = now
                        )
                    )
                    // Always update the time/name fields without touching openCount or scrolls.
                    appUsageDao.updateUsageStats(
                        pkg = packageName,
                        appName = appName,
                        date = today,
                        minutes = minutes,
                        updatedAt = now
                    )
                }
            }
    }

    private fun aggregateForegroundUsage(startMs: Long, endMs: Long): Map<String, Long> {
        val totals = mutableMapOf<String, Long>()
        val activeStarts = mutableMapOf<String, Long>()
        val events = usageStatsManager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            if (packageName == context.packageName) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    activeStarts[packageName] = event.timeStamp
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val startedAt = activeStarts.remove(packageName) ?: continue
                    val duration = (event.timeStamp - startedAt).coerceAtLeast(0L)
                    totals[packageName] = (totals[packageName] ?: 0L) + duration
                }
            }
        }

        activeStarts.forEach { (packageName, startedAt) ->
            val duration = (endMs - startedAt).coerceAtLeast(0L)
            totals[packageName] = (totals[packageName] ?: 0L) + duration
        }

        return totals
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
