package com.mobiletrack.app.presentation.cleanup

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class UnusedAppInfo(
    val appName: String,
    val packageName: String,
    val daysSinceLastUsed: Int, // -1 = never used
    val sizeBytes: Long,
    val lastUsedTimestamp: Long // 0 = never
)

@HiltViewModel
class AppCleanupViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _unusedApps = MutableStateFlow<List<UnusedAppInfo>>(emptyList())
    val unusedApps: StateFlow<List<UnusedAppInfo>> = _unusedApps

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _totalInstalledApps = MutableStateFlow(0)
    val totalInstalledApps: StateFlow<Int> = _totalInstalledApps

    companion object {
        private const val UNUSED_THRESHOLD_DAYS = 14 // Apps not used in 14+ days
    }

    init {
        scanApps()
    }

    private fun scanApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) { findUnusedApps() }
            _unusedApps.value = result
            _isLoading.value = false
        }
    }

    private fun findUnusedApps(): List<UnusedAppInfo> {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // Get all launchable apps
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launchableApps = pm.queryIntentActivities(launchIntent, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != context.packageName }

        _totalInstalledApps.value = launchableApps.size

        // Query usage stats for the last 6 months
        val now = System.currentTimeMillis()
        val sixMonthsAgo = now - 180L * 24 * 60 * 60 * 1000
        val usageStats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_MONTHLY, sixMonthsAgo, now
        )

        // Build map of package → last time used
        val lastUsedMap = mutableMapOf<String, Long>()
        usageStats?.forEach { stat ->
            val existing = lastUsedMap[stat.packageName] ?: 0L
            if (stat.lastTimeUsed > existing) {
                lastUsedMap[stat.packageName] = stat.lastTimeUsed
            }
        }

        val unusedList = mutableListOf<UnusedAppInfo>()
        val thresholdMs = now - UNUSED_THRESHOLD_DAYS.toLong() * 24 * 60 * 60 * 1000

        for (ri in launchableApps) {
            val pkg = ri.activityInfo.packageName
            val appName = ri.loadLabel(pm).toString()
            val lastUsed = lastUsedMap[pkg] ?: 0L

            // Skip if used recently
            if (lastUsed > thresholdMs) continue

            // Get app size
            val sizeBytes = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val sourceDir = appInfo.sourceDir
                if (sourceDir != null) java.io.File(sourceDir).length() else 0L
            } catch (_: Exception) { 0L }

            val daysSinceUsed = if (lastUsed == 0L) -1
            else ((now - lastUsed) / (24 * 60 * 60 * 1000)).toInt()

            unusedList.add(
                UnusedAppInfo(
                    appName = appName,
                    packageName = pkg,
                    daysSinceLastUsed = daysSinceUsed,
                    sizeBytes = sizeBytes,
                    lastUsedTimestamp = lastUsed
                )
            )
        }

        // Sort: never used first, then by staleness (most stale first)
        return unusedList.sortedWith(
            compareByDescending<UnusedAppInfo> { it.daysSinceLastUsed == -1 }
                .thenByDescending { it.daysSinceLastUsed }
        )
    }
}
