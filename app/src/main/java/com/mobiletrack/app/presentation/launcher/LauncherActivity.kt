package com.mobiletrack.app.presentation.launcher

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mobiletrack.app.data.local.dao.AppOpenEventDao
import com.mobiletrack.app.data.local.dao.AppRuleDao
import com.mobiletrack.app.data.local.dao.AppUsageDao
import com.mobiletrack.app.data.local.dao.UnlockDao
import com.mobiletrack.app.data.local.entity.AppOpenEvent
import com.mobiletrack.app.data.local.entity.AppRule
import com.mobiletrack.app.data.local.entity.AppUsageSession
import com.mobiletrack.app.data.preferences.UserPreferences
import com.mobiletrack.app.presentation.blocked.ScrollReminderActivity
import com.mobiletrack.app.presentation.blocked.AppBlockedActivity
import com.mobiletrack.app.presentation.design.MTIconSize
import com.mobiletrack.app.presentation.design.MTRadius
import com.mobiletrack.app.presentation.design.MTSpacing
import com.mobiletrack.app.presentation.MainActivity
import com.mobiletrack.app.presentation.theme.MobileTrackTheme
import com.mobiletrack.app.service.TrackingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// ── Data Models ─────────────────────────────────────────────────────────────

private data class HomeApp(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)

/** Enriched app state combining install info with usage/rules. */
private data class AppDisplayState(
    val usageMinutes: Int = 0,
    val dailyLimitMinutes: Int = 0,
    val isBlocked: Boolean = false,
    val isLimitReached: Boolean = false,
    val isAddictive: Boolean = false
) {
    val isRestricted get() = isBlocked || isLimitReached
}

// ── Addictive app packages (shared with BlockerAccessibilityService) ────────

private val ADDICTIVE_PACKAGES = setOf(
    "com.instagram.android",
    "com.google.android.youtube",
    "com.zhiliaoapp.musically",   // TikTok
    "com.snapchat.android",
    "com.twitter.android",
    "com.reddit.frontpage",
    "com.facebook.katana"
)

// ── Quiet hours ─────────────────────────────────────────────────────────────

private const val QUIET_HOURS_NIGHT_START = 22
private const val QUIET_HOURS_MORNING_END = 7

private fun isQuietHours(): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return hour >= QUIET_HOURS_NIGHT_START || hour < QUIET_HOURS_MORNING_END
}

// ── Activity ────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    @Inject lateinit var appOpenEventDao: AppOpenEventDao
    @Inject lateinit var appUsageDao: AppUsageDao
    @Inject lateinit var appRuleDao: AppRuleDao
    @Inject lateinit var unlockDao: UnlockDao
    @Inject lateinit var userPreferences: UserPreferences

    private var apps by mutableStateOf<List<HomeApp>>(emptyList())
    private var dockPackages by mutableStateOf<List<String>>(emptyList())
    private lateinit var launcherPrefs: SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureEdgeToEdgeSystemBars()
        startForegroundService(Intent(this, TrackingService::class.java))
        launcherPrefs = getSharedPreferences(LAUNCHER_PREFS, MODE_PRIVATE)
        dockPackages = loadDockPackages()

        val today = dateFormat.format(Date())
        val startOfDayMs = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Reactive flows for live data
        val usageFlow = appUsageDao.getUsageForDate(today)
        val rulesFlow = appRuleDao.getAllRules()
        val totalMinutesFlow = appUsageDao.getTotalMinutesForDate(today).map { it ?: 0 }
        val unlockCountFlow = unlockDao.countSince(startOfDayMs)
        val streakFlow = userPreferences.streakDays

        setContent {
            MobileTrackTheme(darkTheme = true) {
                val usageList by usageFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                val rulesList by rulesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                val totalMinutes by totalMinutesFlow.collectAsStateWithLifecycle(initialValue = 0)
                val unlockCount by unlockCountFlow.collectAsStateWithLifecycle(initialValue = 0)
                val streakDays by streakFlow.collectAsStateWithLifecycle(initialValue = 0)

                // Build lookup maps
                val usageMap = remember(usageList) {
                    usageList.associateBy { it.packageName }
                }
                val rulesMap = remember(rulesList) {
                    rulesList.associateBy { it.packageName }
                }

                OneUiInspiredLauncher(
                    apps = apps,
                    dockPackages = dockPackages,
                    usageMap = usageMap,
                    rulesMap = rulesMap,
                    totalMinutes = totalMinutes,
                    unlockCount = unlockCount,
                    streakDays = streakDays,
                    onOpenApp = ::openApp,
                    onDockPackageChanged = ::updateDockPackage,
                    onOpenMobileTrack = ::openMobileTrack,
                    onAppInfo = { app ->
                        val infoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${app.packageName}")
                        }
                        startActivity(infoIntent)
                    },
                    onUninstall = { app ->
                        val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.parse("package:${app.packageName}")
                        }
                        startActivity(uninstallIntent)
                    }
                )
            }
        }

        lifecycleScope.launch {
            apps = withContext(Dispatchers.IO) { loadLaunchableApps() }
        }
    }

    private fun loadLaunchableApps(): List<HomeApp> {
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager.queryIntentActivities(launchIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map { resolveInfo ->
                HomeApp(
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    private fun openApp(app: HomeApp) {
        // --- Quiet hours: block addictive apps entirely ---
        if (isQuietHours() && app.packageName in ADDICTIVE_PACKAGES) {
            val intent = Intent(this, ScrollReminderActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("app_name", app.appName)
                putExtra("reason", "quiet_hours")
            }
            startActivity(intent)
            return
        }

        // --- Check hard-block and time limit ---
        lifecycleScope.launch {
            val rule = withContext(Dispatchers.IO) { appRuleDao.getRuleForApp(app.packageName) }
            if (rule != null) {
                if (rule.isBlocked) {
                    launchBlockedScreen(app.appName, isLimitReached = false)
                    return@launch
                }
                if (rule.dailyLimitMinutes > 0) {
                    val today = dateFormat.format(Date())
                    val used = withContext(Dispatchers.IO) {
                        appUsageDao.getSessionForApp(app.packageName, today)?.totalMinutes ?: 0
                    }
                    if (used >= rule.dailyLimitMinutes) {
                        launchBlockedScreen(app.appName, isLimitReached = true)
                        return@launch
                    }
                }
            }

            // --- Normal launch ---
            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return@launch
            recordLauncherOpen(app)
            startActivity(launchIntent)
        }
    }

    private fun launchBlockedScreen(appName: String, isLimitReached: Boolean) {
        val intent = Intent(this, AppBlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("app_name", appName)
            putExtra("is_limit_reached", isLimitReached)
        }
        startActivity(intent)
    }

    private fun openMobileTrack() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun configureEdgeToEdgeSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun loadDockPackages(): List<String> {
        return launcherPrefs.getString(KEY_DOCK_PACKAGES, null)
            ?.split(DOCK_SEPARATOR)
            ?.filter { it.isNotBlank() }
            .orEmpty()
    }

    private fun updateDockPackage(slotIndex: Int, packageName: String) {
        val next = dockPackages.toMutableList()
        while (next.size < DOCK_SIZE) next.add("")
        next[slotIndex] = packageName
        dockPackages = next
        launcherPrefs.edit()
            .putString(KEY_DOCK_PACKAGES, next.joinToString(DOCK_SEPARATOR))
            .apply()
    }

    private fun recordLauncherOpen(app: HomeApp) {
        if (app.packageName == packageName) return
        launcherPrefs.edit()
            .putString(KEY_LAST_LAUNCHER_OPEN_PACKAGE, app.packageName)
            .putLong(KEY_LAST_LAUNCHER_OPEN_AT, System.currentTimeMillis())
            .apply()
        lifecycleScope.launch(Dispatchers.IO) {
            val today = dateFormat.format(Date())
            appUsageDao.insertIfAbsent(
                AppUsageSession(
                    packageName = app.packageName,
                    appName = app.appName,
                    date = today,
                    totalMinutes = 0,
                    openCount = 0
                )
            )
            appUsageDao.incrementOpenCount(app.packageName, today)
            appOpenEventDao.insert(AppOpenEvent(packageName = app.packageName, appName = app.appName))
        }
    }
}

// ── Composables ─────────────────────────────────────────────────────────────

@Composable
private fun OneUiInspiredLauncher(
    apps: List<HomeApp>,
    dockPackages: List<String>,
    usageMap: Map<String, AppUsageSession>,
    rulesMap: Map<String, AppRule>,
    totalMinutes: Int,
    unlockCount: Int,
    streakDays: Int,
    onOpenApp: (HomeApp) -> Unit,
    onDockPackageChanged: (slotIndex: Int, packageName: String) -> Unit,
    onOpenMobileTrack: () -> Unit,
    onAppInfo: (HomeApp) -> Unit,
    onUninstall: (HomeApp) -> Unit
) {
    var drawerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var editingDockSlot by remember { mutableStateOf<Int?>(null) }
    var timeText by remember { mutableStateOf(currentTimeText()) }
    val dateText = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d")) }
    val dockApps = remember(apps, dockPackages) { resolveDockApps(apps, dockPackages) }
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps else apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }

    // Build display state for each app
    val displayStates = remember(usageMap, rulesMap) {
        buildDisplayStates(usageMap, rulesMap)
    }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = currentTimeText()
            kotlinx.coroutines.delay(30_000)
        }
    }

    BackHandler(enabled = drawerOpen) {
        drawerOpen = false
        searchQuery = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(oneUiWallpaperBrush())
    ) {
        HomePage(
            dateText = dateText,
            timeText = timeText,
            totalMinutes = totalMinutes,
            unlockCount = unlockCount,
            streakDays = streakDays,
            dockApps = dockApps,
            displayStates = displayStates,
            onOpenApp = onOpenApp,
            onEditDockSlot = { editingDockSlot = it },
            onOpenDrawer = { drawerOpen = true },
            onOpenMobileTrack = onOpenMobileTrack,
            onAppInfo = onAppInfo,
            onUninstall = onUninstall
        )

        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
        ) {
            AppDrawer(
                apps = filteredApps,
                displayStates = displayStates,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onOpenApp = onOpenApp,
                onAppInfo = onAppInfo,
                onUninstall = onUninstall,
                onClose = {
                    drawerOpen = false
                    searchQuery = ""
                }
            )
        }

        AnimatedVisibility(
            visible = editingDockSlot != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
        ) {
            DockPicker(
                apps = apps,
                slotIndex = editingDockSlot ?: 0,
                onSelect = { slotIndex, app ->
                    onDockPackageChanged(slotIndex, app.packageName)
                    editingDockSlot = null
                },
                onClose = { editingDockSlot = null }
            )
        }
    }
}

// ── Home Page ───────────────────────────────────────────────────────────────

@Composable
private fun HomePage(
    dateText: String,
    timeText: String,
    totalMinutes: Int,
    unlockCount: Int,
    streakDays: Int,
    dockApps: List<HomeApp>,
    displayStates: Map<String, AppDisplayState>,
    onOpenApp: (HomeApp) -> Unit,
    onEditDockSlot: (Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenMobileTrack: () -> Unit,
    onAppInfo: (HomeApp) -> Unit,
    onUninstall: (HomeApp) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = MTSpacing.lg, vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Time & date
            Text(
                text = timeText,
                color = Color.White,
                fontSize = 54.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp
            )
            Text(
                text = dateText,
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(MTSpacing.md))

            // ── Dashboard widget ────────────────────────────────────────
            DashboardWidget(
                totalMinutes = totalMinutes,
                unlockCount = unlockCount,
                streakDays = streakDays,
                onClick = onOpenMobileTrack
            )

            Spacer(Modifier.height(MTSpacing.md))

            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MTRadius.xl))
                    .background(glassSurfaceBrush(alphaTop = 0.22f, alphaBottom = 0.10f))
                    .clickable(onClick = onOpenDrawer)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Search apps",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Dock
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MTRadius.xl))
                    .background(glassSurfaceBrush(alphaTop = 0.24f, alphaBottom = 0.11f))
                    .padding(horizontal = MTSpacing.sm, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(MTSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockApps.take(DOCK_SIZE).forEachIndexed { index, app ->
                    val state = displayStates[app.packageName] ?: AppDisplayState()
                    LauncherIcon(
                        app = app,
                        displayState = state,
                        modifier = Modifier.weight(1f),
                        iconSize = 48.dp,
                        showLabel = false,
                        showBadge = true,
                        onLongClick = { onEditDockSlot(index) },
                        onAppInfo = onAppInfo,
                        onUninstall = onUninstall,
                        onClick = { onOpenApp(app) }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "MobileTrack dashboard",
                modifier = Modifier
                    .clip(RoundedCornerShape(MTRadius.lg))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .clickable(onClick = onOpenMobileTrack)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ── Dashboard Widget ────────────────────────────────────────────────────────

@Composable
private fun DashboardWidget(
    totalMinutes: Int,
    unlockCount: Int,
    streakDays: Int,
    onClick: () -> Unit
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(hour) {
        when {
            hour < 6 -> "It's late — rest well"
            hour < 12 -> "Start your day with intention"
            hour < 17 -> "Stay focused this afternoon"
            hour < 21 -> "Wind down mindfully"
            else -> "Screen off soon — you've got this"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(MTRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x28FFFFFF)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = greeting,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Screen time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color(0xFF90CAF9),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = formatMinutesShort(totalMinutes),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Unlocks
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFFCE93D8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$unlockCount unlocks",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Streak
                if (streakDays > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFFAB40),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${streakDays}d",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// ── App Drawer ──────────────────────────────────────────────────────────────

@Composable
private fun AppDrawer(
    apps: List<HomeApp>,
    displayStates: Map<String, AppDisplayState>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenApp: (HomeApp) -> Unit,
    onAppInfo: (HomeApp) -> Unit,
    onUninstall: (HomeApp) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF15171C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = MTSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(MTRadius.pill),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Done",
                    modifier = Modifier
                        .clip(RoundedCornerShape(MTRadius.lg))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    color = Color(0xFF0B57D0),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(MTSpacing.lg))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val state = displayStates[app.packageName] ?: AppDisplayState()
                    LauncherIcon(
                        app = app,
                        displayState = state,
                        darkLabel = false,
                        showBadge = true,
                        onAppInfo = onAppInfo,
                        onUninstall = onUninstall,
                        onClick = { onOpenApp(app) }
                    )
                }
            }
        }
    }
}

// ── Dock Picker ─────────────────────────────────────────────────────────────

@Composable
private fun DockPicker(
    apps: List<HomeApp>,
    slotIndex: Int,
    onSelect: (slotIndex: Int, app: HomeApp) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.appName.contains(query, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xF20B0C10)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = MTSpacing.md)
        ) {
            Text(
                text = "Choose dock app ${slotIndex + 1}",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(MTSpacing.md))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(MTRadius.pill),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Cancel",
                    modifier = Modifier
                        .clip(RoundedCornerShape(MTRadius.lg))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    color = Color.White.copy(alpha = 0.86f),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(MTSpacing.lg))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    LauncherIcon(
                        app = app,
                        displayState = AppDisplayState(),
                        onClick = { onSelect(slotIndex, app) }
                    )
                }
            }
        }
    }
}

// ── Launcher Icon with badge, dim, and lock overlay ─────────────────────────

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LauncherIcon(
    app: HomeApp,
    displayState: AppDisplayState,
    modifier: Modifier = Modifier,
    iconSize: Dp = MTIconSize.launcher,
    darkLabel: Boolean = false,
    showLabel: Boolean = true,
    showBadge: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onAppInfo: ((HomeApp) -> Unit)? = null,
    onUninstall: ((HomeApp) -> Unit)? = null,
    onClick: () -> Unit
) {
    val isRestricted = displayState.isRestricted
    val iconAlpha = if (isRestricted) 0.35f else 1f
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (onAppInfo != null || onUninstall != null) {
                        showMenu = true
                    }
                    onLongClick?.invoke()
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                update = { imageView ->
                    imageView.setImageDrawable(app.icon)
                    imageView.alpha = iconAlpha
                    if (isRestricted) {
                        imageView.colorFilter = android.graphics.ColorMatrixColorFilter(
                            android.graphics.ColorMatrix().apply { setSaturation(0f) }
                        )
                    } else {
                        imageView.colorFilter = null
                    }
                },
                modifier = Modifier
                    .size(iconSize)
                    .clip(RoundedCornerShape(MTRadius.md))
            )

            // Lock overlay for restricted apps
            if (isRestricted) {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(MTRadius.md))
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Blocked",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Context menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = Color(0xFF1A1E2E)
            ) {
                if (onAppInfo != null) {
                    DropdownMenuItem(
                        text = {
                            Text("App Info", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = Color(0xFF7B8CB8), modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            showMenu = false
                            onAppInfo(app)
                        }
                    )
                }
                if (onUninstall != null) {
                    DropdownMenuItem(
                        text = {
                            Text("Uninstall", color = Color(0xFFEF5350), style = MaterialTheme.typography.bodyMedium)
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            showMenu = false
                            onUninstall(app)
                        }
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(Modifier.height(7.dp))
            Text(
                text = app.appName,
                color = if (darkLabel) Color(0xFF17171A)
                else if (isRestricted) Color.White.copy(alpha = 0.4f)
                else Color.White.copy(alpha = 0.94f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }

        // Usage badge
        if (showBadge && displayState.usageMinutes > 0) {
            Spacer(Modifier.height(2.dp))
            val badgeColor = when {
                displayState.isLimitReached -> Color(0xFFEF5350)  // red
                displayState.usageMinutes >= 60 -> Color(0xFFFF7043) // orange
                displayState.usageMinutes >= 30 -> Color(0xFFFFCA28) // amber
                else -> Color(0xFF66BB6A) // green
            }
            Text(
                text = formatMinutesShort(displayState.usageMinutes),
                color = badgeColor,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun buildDisplayStates(
    usageMap: Map<String, AppUsageSession>,
    rulesMap: Map<String, AppRule>
): Map<String, AppDisplayState> {
    val allPackages = (usageMap.keys + rulesMap.keys)
    return allPackages.associateWith { pkg ->
        val usage = usageMap[pkg]
        val rule = rulesMap[pkg]
        val minutes = usage?.totalMinutes ?: 0
        val limit = rule?.dailyLimitMinutes ?: 0
        val blocked = rule?.isBlocked == true
        val limitReached = limit > 0 && minutes >= limit
        AppDisplayState(
            usageMinutes = minutes,
            dailyLimitMinutes = limit,
            isBlocked = blocked,
            isLimitReached = limitReached,
            isAddictive = pkg in ADDICTIVE_PACKAGES
        )
    }
}

private fun formatMinutesShort(minutes: Int): String {
    val clamped = minutes.coerceAtLeast(0)
    val h = clamped / 60
    val m = clamped % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun oneUiWallpaperBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color(0xFF050608),
            Color(0xFF15171C),
            Color(0xFF2B2E36),
            Color(0xFF111318)
        )
    )
}

private fun glassSurfaceBrush(alphaTop: Float, alphaBottom: Float): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = alphaTop),
            Color.White.copy(alpha = alphaBottom)
        )
    )
}

private fun resolveDockApps(apps: List<HomeApp>, savedPackages: List<String>): List<HomeApp> {
    val savedApps = savedPackages.mapNotNull { packageName ->
        apps.firstOrNull { it.packageName == packageName }
    }
    return (savedApps + pickDefaultDockApps(apps)).distinctBy { it.packageName }.take(DOCK_SIZE)
}

private fun pickDefaultDockApps(apps: List<HomeApp>): List<HomeApp> {
    val preferred = listOf("phone", "contacts", "message", "camera", "chrome", "gallery")
    val picked = preferred.mapNotNull { token ->
        apps.firstOrNull { app ->
            app.appName.contains(token, ignoreCase = true) ||
                app.packageName.contains(token, ignoreCase = true)
        }
    }.distinctBy { it.packageName }
    return (picked + apps).distinctBy { it.packageName }.take(DOCK_SIZE)
}

private fun currentTimeText(): String {
    return LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm"))
}

private const val LAUNCHER_PREFS = "launcher_preferences"
private const val KEY_DOCK_PACKAGES = "dock_packages"
private const val KEY_LAST_LAUNCHER_OPEN_PACKAGE = "last_launcher_open_package"
private const val KEY_LAST_LAUNCHER_OPEN_AT = "last_launcher_open_at"
private const val DOCK_SEPARATOR = "|"
private const val DOCK_SIZE = 4
