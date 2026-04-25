package com.mobiletrack.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.mobiletrack.app.data.local.dao.AppOpenEventDao
import com.mobiletrack.app.data.local.dao.AppRuleDao
import com.mobiletrack.app.data.local.dao.AppUsageDao
import com.mobiletrack.app.data.local.dao.UnlockDao
import com.mobiletrack.app.data.local.entity.AppOpenEvent
import com.mobiletrack.app.data.local.entity.AppUsageSession
import com.mobiletrack.app.data.preferences.UserPreferences
import com.mobiletrack.app.presentation.blocked.AppBlockedActivity
import com.mobiletrack.app.presentation.blocked.ScrollReminderActivity
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
    @Inject lateinit var unlockDao: UnlockDao
    @Inject lateinit var userPreferences: UserPreferences

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
    private val CHECK_INTERVAL_MS = 15_000L // re-check every 15 seconds
    private var activeCheckJob: Job? = null

    // --- Burst scroll detection: 20 scrolls in 5 minutes ---
    private val scrollTimestamps = mutableListOf<Long>()
    private val BURST_SCROLL_LIMIT = 20
    private val BURST_WINDOW_MS = 5 * 60 * 1000L // 5 minutes
    private var lastBurstReminderAt = 0L
    private val BURST_REMINDER_COOLDOWN_MS = 60_000L // 1 minute between reminders

    // --- Temporary app lock after burst/quiet hours (5 minutes) ---
    // Maps packageName -> unlock timestamp (when the lock expires)
    private val tempLockedApps = mutableMapOf<String, Long>()
    private val TEMP_LOCK_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    // --- Quiet hours: morning & late night scroll prohibition ---
    private val QUIET_HOURS_NIGHT_START = 22  // 10 PM
    private val QUIET_HOURS_MORNING_END = 7   // 7 AM
    private var lastQuietHoursReminderAt = 0L
    private val QUIET_HOURS_REMINDER_COOLDOWN_MS = 30_000L // 30 seconds between reminders

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: return
                if (pkg == packageName) return

                // Fallback unlock detection: transitioning from lock screen to a real app
                checkForUnlockTransition(pkg)

                // --- Temp lock: if this app was locked after burst scrolling, block re-entry ---
                val lockExpiry = tempLockedApps[pkg]
                if (lockExpiry != null) {
                    val now = System.currentTimeMillis()
                    if (now < lockExpiry) {
                        val remainingMs = lockExpiry - now
                        val appName = getAppLabel(pkg)
                        launchScrollReminder(pkg, appName, "temp_locked", remainingMs)
                        return
                    } else {
                        tempLockedApps.remove(pkg)
                    }
                }

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
                scrollTimestamps.clear()

                scope.launch {
                    val today = dateFormat.format(Date())
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
                if (pkg !in scrollAdditivePackages) return

                // --- Quiet hours check: strictly block scrolling during morning/night ---
                if (isQuietHours()) {
                    val now = System.currentTimeMillis()
                    if (now - lastQuietHoursReminderAt > QUIET_HOURS_REMINDER_COOLDOWN_MS) {
                        lastQuietHoursReminderAt = now
                        tempLockedApps[pkg] = now + TEMP_LOCK_DURATION_MS
                        val appName = getAppLabel(pkg)
                        launchScrollReminder(pkg, appName, "quiet_hours", TEMP_LOCK_DURATION_MS)
                    }
                    return
                }

                scrollCountInSession++

                // --- Burst scroll detection: 20 scrolls in 5 minutes ---
                val now = System.currentTimeMillis()
                scrollTimestamps.add(now)
                // Prune timestamps older than the window
                val cutoff = now - BURST_WINDOW_MS
                scrollTimestamps.removeAll { it < cutoff }

                if (scrollTimestamps.size >= BURST_SCROLL_LIMIT) {
                    if (now - lastBurstReminderAt > BURST_REMINDER_COOLDOWN_MS) {
                        lastBurstReminderAt = now
                        scrollTimestamps.clear()
                        // Lock this app for 5 minutes
                        tempLockedApps[pkg] = now + TEMP_LOCK_DURATION_MS
                        val appName = getAppLabel(pkg)
                        launchScrollReminder(pkg, appName, "burst", TEMP_LOCK_DURATION_MS)
                    }
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

    private fun isQuietHours(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Quiet hours: 10 PM (22) through 6:59 AM (before 7)
        return hour >= QUIET_HOURS_NIGHT_START || hour < QUIET_HOURS_MORNING_END
    }

    private fun getAppLabel(pkg: String): String {
        return runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        }.getOrDefault(pkg)
    }

    private val LOCK_SCREEN_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.keyguard"
    )

    private fun checkForUnlockTransition(newPackage: String) {
        // Only treat it as an unlock if the device was actually locked.
        // com.android.systemui also appears for notifications, volume panel, quick settings —
        // those are NOT unlock transitions.
        val wasPossiblyLocked = currentPackage == null || currentPackage == "com.android.keyguard"
        if (wasPossiblyLocked && newPackage !in LOCK_SCREEN_PACKAGES) {
            val keyguardManager = getSystemService(android.content.Context.KEYGUARD_SERVICE)
                as android.app.KeyguardManager
            // Double-check: only fire if the keyguard just cleared
            if (!keyguardManager.isDeviceLocked) {
                scope.launch {
                    UnlockHandler.handleUnlock(
                        this@BlockerAccessibilityService,
                        "accessibility_fallback",
                        unlockDao,
                        userPreferences
                    )
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

        // Time limit check — query fresh totalMinutes from DB (kept up-to-date by UsageTracker).
        if (rule.dailyLimitMinutes > 0) {
            val today = dateFormat.format(Date())
            val totalMinutes = appUsageDao.getSessionForApp(packageName, today)?.totalMinutes ?: 0
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

    private fun launchScrollReminder(packageName: String, appName: String, reason: String, lockRemainingMs: Long = 0L) {
        // Go home first to reliably close the offending app
        performGlobalAction(GLOBAL_ACTION_HOME)

        val intent = Intent(this, ScrollReminderActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("package_name", packageName)
            putExtra("app_name", appName)
            putExtra("reason", reason)
            putExtra("lock_remaining_ms", lockRemainingMs)
        }
        startActivity(intent)
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
