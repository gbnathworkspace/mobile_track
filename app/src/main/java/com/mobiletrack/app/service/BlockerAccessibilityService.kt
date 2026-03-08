package com.mobiletrack.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.mobiletrack.app.data.local.dao.AppRuleDao
import com.mobiletrack.app.data.local.dao.AppUsageDao
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
    private var scrollCountInSession = 0
    private val SCROLL_NUDGE_THRESHOLD = 30  // nudge after 30 scroll events

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: return
                if (pkg == currentPackage) return
                currentPackage = pkg
                scrollCountInSession = 0
                scope.launch { checkAndBlock(pkg) }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val pkg = event.packageName?.toString() ?: return
                if (pkg in scrollAdditivePackages) {
                    scrollCountInSession++
                    if (scrollCountInSession % SCROLL_NUDGE_THRESHOLD == 0) {
                        showScrollNudge(pkg)
                    }
                }
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

        // Time limit check
        if (rule.dailyLimitMinutes > 0) {
            val today = dateFormat.format(Date())
            val session = appUsageDao.getSessionForApp(packageName, today)
            if (session != null && session.totalMinutes >= rule.dailyLimitMinutes) {
                launchBlockedScreen(packageName, rule.appName, isLimitReached = true)
            }
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

    override fun onInterrupt() {
        scope.cancel()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
