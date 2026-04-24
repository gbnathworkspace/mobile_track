package com.mobiletrack.app.presentation.launcher

import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.mobiletrack.app.data.local.dao.AppOpenEventDao
import com.mobiletrack.app.data.local.dao.AppUsageDao
import com.mobiletrack.app.data.local.entity.AppOpenEvent
import com.mobiletrack.app.data.local.entity.AppUsageSession
import com.mobiletrack.app.presentation.design.MTIconSize
import com.mobiletrack.app.presentation.design.MTRadius
import com.mobiletrack.app.presentation.design.MTSpacing
import com.mobiletrack.app.presentation.MainActivity
import com.mobiletrack.app.presentation.theme.MobileTrackTheme
import com.mobiletrack.app.service.TrackingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private data class HomeApp(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    @Inject lateinit var appOpenEventDao: AppOpenEventDao
    @Inject lateinit var appUsageDao: AppUsageDao

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

        setContent {
            MobileTrackTheme(darkTheme = true) {
                OneUiInspiredLauncher(
                    apps = apps,
                    dockPackages = dockPackages,
                    onOpenApp = ::openApp,
                    onDockPackageChanged = ::updateDockPackage,
                    onOpenMobileTrack = ::openMobileTrack
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
            .filter { resolveInfo -> resolveInfo.activityInfo.packageName != packageName }
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
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        recordLauncherOpen(app)
        startActivity(launchIntent)
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

@Composable
private fun OneUiInspiredLauncher(
    apps: List<HomeApp>,
    dockPackages: List<String>,
    onOpenApp: (HomeApp) -> Unit,
    onDockPackageChanged: (slotIndex: Int, packageName: String) -> Unit,
    onOpenMobileTrack: () -> Unit
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
            dockApps = dockApps,
            onOpenApp = onOpenApp,
            onEditDockSlot = { editingDockSlot = it },
            onOpenDrawer = { drawerOpen = true },
            onOpenMobileTrack = onOpenMobileTrack
        )

        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
        ) {
            AppDrawer(
                apps = filteredApps,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onOpenApp = onOpenApp,
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

@Composable
private fun HomePage(
    dateText: String,
    timeText: String,
    dockApps: List<HomeApp>,
    onOpenApp: (HomeApp) -> Unit,
    onEditDockSlot: (Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenMobileTrack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = MTSpacing.lg, vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
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

            Spacer(Modifier.height(MTSpacing.lg))

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
                    LauncherIcon(
                        app = app,
                        modifier = Modifier.weight(1f),
                        iconSize = 48.dp,
                        showLabel = false,
                        onLongClick = { onEditDockSlot(index) },
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

@Composable
private fun AppDrawer(
    apps: List<HomeApp>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenApp: (HomeApp) -> Unit,
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
                    LauncherIcon(
                        app = app,
                        darkLabel = false,
                        onClick = { onOpenApp(app) }
                    )
                }
            }
        }
    }
}

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
                        onClick = { onSelect(slotIndex, app) }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LauncherIcon(
    app: HomeApp,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = MTIconSize.launcher,
    darkLabel: Boolean = false,
    showLabel: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            },
            update = { imageView -> imageView.setImageDrawable(app.icon) },
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(MTRadius.md))
        )
        if (showLabel) {
            Spacer(Modifier.height(7.dp))
            Text(
                text = app.appName,
                color = if (darkLabel) Color(0xFF17171A) else Color.White.copy(alpha = 0.94f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
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
