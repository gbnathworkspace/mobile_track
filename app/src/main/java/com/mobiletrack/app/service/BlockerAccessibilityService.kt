package com.mobiletrack.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.mobiletrack.app.data.local.dao.AppOpenEventDao
import com.mobiletrack.app.data.local.dao.AppRuleDao
import com.mobiletrack.app.data.local.dao.AppUsageDao
import com.mobiletrack.app.data.local.entity.AppOpenEvent
import com.mobiletrack.app.data.local.entity.AppUsageSession
import com.mobiletrack.app.presentation.blocked.AppBlockedActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class BlockerAccessibilityService : AccessibilityService() {

    @Inject lateinit var appRuleDao: AppRuleDao
    @Inject lateinit var appUsageDao: AppUsageDao
    @Inject lateinit var appOpenEventDao: AppOpenEventDao

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Addictive apps with infinite scroll — tracked for scroll nudges
    private val scrollAdditivePackages = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.zhiliaoapp.musically",   // TikTok
        "com.snapchat.android",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.facebook.katana"
    )

    private var currentPackage: String? = null
    private var sessionStartTime: Long = 0L
    private var scrollCountInSession = 0
    // Snapshot of DB minutes taken when the session starts; used in checkAndBlock() to avoid
    // double-counting with the UsageTracker that continuously updates the same DB row.
    private var baselineMinutes: Int = 0
    private val SCROLL_NUDGE_THRESHOLD = 30
    private val CHECK_INTERVAL_MS = 15_000L // re-check every 15 seconds
    private var activeCheckJob: Job? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: return
                if (pkg == packageName) return
                if (pkg == currentPackage) return

                // Flush any unsaved scrolls for the app we're leaving before resetting state.
                val prevPkg = currentPackage
                val unsavedScrolls = scrollCountInSession % 5
                if (unsavedScrolls > 0 && prevPkg != null && prevPkg in scrollAdditivePackages) {
                    scope.launch {
                        appUsageDao.incrementScrolls(prevPkg, dateFormat.format(Date()), unsavedScrolls)
                    }
                }

                currentPackage = pkg
                sessionStartTime = System.currentTimeMillis()
                scrollCountInSession = 0

                scope.launch {
                    val today = dateFormat.format(Date())
                    // Snapshot pre-session minutes so checkAndBlock() doesn't double-count with
                    // the UsageTracker that continuously upserts totalMinutes from the system.
                    baselineMinutes = appUsageDao.getSessionForApp(pkg, today)?.totalMinutes ?: 0

                    val appName = runCatching {
                        packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(pkg, 0)
                        ).toString()
                    }.getOrDefault(pkg)
                    if (!isRecentLauncherOpen(pkg)) {
                        appUsageDao.insertIfAbsent(
                            AppUsageSession(
                                packageName = pkg,
                                appName = appName,
                                date = today,
                                totalMinutes = 0,
                                openCount = 0
                            )
                        )
                        appOpenEventDao.insert(AppOpenEvent(packageName = pkg, appName = appName))
                        appUsageDao.incrementOpenCount(pkg, today)
                    }
                }
                startActiveCheck(pkg)
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val pkg = event.packageName?.toString() ?: return
                if (pkg in scrollAdditivePackages) {
                    scrollCountInSession++
                    if (scrollCountInSession % SCROLL_NUDGE_THRESHOLD == 0) {
                        showScrollNudge(pkg)
                    }
                    // Persist every 5 scrolls to avoid too many DB writes
                    if (scrollCountInSession % 5 == 0) {
                        scope.launch {
                            val today = dateFormat.format(Date())
                            appUsageDao.incrementScrolls(pkg, today, 5)
                        }
                    }
                    scope.launch { checkScrollLimit(pkg) }
                }
            }
        }
    }

    private fun startActiveCheck(packageName: String) {
        activeCheckJob?.cancel()
        activeCheckJob = scope.launch {
            while (isActive && currentPackage == packageName) {
                checkAndBlock(packageName)
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkAndBlock(packageName: String) {
        val rule = appRuleDao.getRuleForApp(packageName) ?: return

        // Hard block
        if (rule.isBlocked) {
            launchBlockedScreen(packageName, rule.appName)
            return
        }

        // Time limit check — baseline snapshot (taken at session start) + elapsed session time.
        // Avoids double-counting with UsageTracker which continuously updates totalMinutes in DB.
        if (rule.dailyLimitMinutes > 0) {
            val sessionMinutes = ((System.currentTimeMillis() - sessionStartTime) / 60_000).toInt()
            val totalMinutes = baselineMinutes + sessionMinutes
            if (totalMinutes >= rule.dailyLimitMinutes) {
                launchBlockedScreen(packageName, rule.appName, isLimitReached = true)
            }
        }
    }

    private suspend fun checkScrollLimit(packageName: String) {
        val rule = appRuleDao.getRuleForApp(packageName) ?: return
        if (rule.dailyScrollLimit <= 0) return
        val today = dateFormat.format(Date())
        val savedScrolls = appUsageDao.getScrollsForApp(packageName, today) ?: 0
        val totalScrolls = savedScrolls + (scrollCountInSession % 5) // include unsaved scrolls
        if (totalScrolls >= rule.dailyScrollLimit) {
            launchBlockedScreen(packageName, rule.appName, isLimitReached = true)
        }
    }

    private fun launchBlockedScreen(packageName: String, appName: String, isLimitReached: Boolean = false) {
        val intent = Intent(this, AppBlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("package_name", packageName)
            putExtra("app_name", appName)
            putExtra("is_limit_reached", isLimitReached)
        }
        startActivity(intent)
    }

    private fun showScrollNudge(packageName: String) {
        // Show a heads-up notification nudging the user
        // Implemented in NotificationHelper
    }

    private fun isRecentLauncherOpen(packageName: String): Boolean {
        val prefs = getSharedPreferences(LAUNCHER_PREFS, MODE_PRIVATE)
        val lastPackage = prefs.getString(KEY_LAST_LAUNCHER_OPEN_PACKAGE, null)
        val lastOpenedAt = prefs.getLong(KEY_LAST_LAUNCHER_OPEN_AT, 0L)
        return lastPackage == packageName &&
            System.currentTimeMillis() - lastOpenedAt < LAUNCHER_OPEN_DEDUPE_MS
    }

    override fun onInterrupt() {
        activeCheckJob?.cancel()
        scope.cancel()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

private const val LAUNCHER_PREFS = "launcher_preferences"
private const val KEY_LAST_LAUNCHER_OPEN_PACKAGE = "last_launcher_open_package"
private const val KEY_LAST_LAUNCHER_OPEN_AT = "last_launcher_open_at"
private const val LAUNCHER_OPEN_DEDUPE_MS = 2_500L
