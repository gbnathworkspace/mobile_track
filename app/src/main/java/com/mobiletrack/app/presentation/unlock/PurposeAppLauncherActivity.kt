package com.mobiletrack.app.presentation.unlock

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.lifecycleScope
import com.mobiletrack.app.data.local.dao.AppOpenEventDao
import com.mobiletrack.app.data.local.entity.AppOpenEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LaunchableApp(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)

@AndroidEntryPoint
class PurposeAppLauncherActivity : ComponentActivity() {

    @Inject lateinit var appOpenEventDao: AppOpenEventDao

    private var apps by mutableStateOf<List<LaunchableApp>>(emptyList())
    private var isLoading by mutableStateOf(true)

    companion object {
        @Volatile private var allAppsCache: List<LaunchableApp>? = null
        private val purposeAppsCache = mutableMapOf<String, List<LaunchableApp>>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val purpose = intent.getStringExtra(EXTRA_PURPOSE).orEmpty()
        Log.d("PurposeAppLauncher", "onCreate purpose=$purpose")

        // Match the unlock prompt's immersive style
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContent {
            MaterialTheme {
                PurposeAppLauncherScreen(
                    purpose = purpose,
                    apps = apps,
                    isLoading = isLoading,
                    onOpenApp = ::openApp,
                    onBack = ::finish
                )
            }
        }

        lifecycleScope.launch {
            apps = withContext(Dispatchers.IO) { loadInstalledApps(purpose) }
            isLoading = false
        }
    }

    private fun loadInstalledApps(purpose: String): List<LaunchableApp> {
        if (purpose == PURPOSE_SEARCH_APP) {
            allAppsCache?.let { return it }
        } else {
            synchronized(purposeAppsCache) {
                purposeAppsCache[purpose]?.let { return it }
            }
            allAppsCache?.let { cached ->
                return cached.filter { app -> matchesPurpose(app.packageName, purpose) }
                    .also { matched ->
                        synchronized(purposeAppsCache) {
                            purposeAppsCache[purpose] = matched
                        }
                    }
            }
        }

        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(launchIntent, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { resolveInfo ->
                purpose == PURPOSE_SEARCH_APP ||
                    matchesPurpose(resolveInfo.activityInfo.packageName, purpose)
            }

        return resolveInfos
            .map { resolveInfo ->
                LaunchableApp(
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }
            .sortedBy { it.appName.lowercase() }
            .also { loadedApps ->
                if (purpose == PURPOSE_SEARCH_APP) {
                    allAppsCache = loadedApps
                } else {
                    synchronized(purposeAppsCache) {
                        purposeAppsCache[purpose] = loadedApps
                    }
                }
            }
    }

    private fun openApp(app: LaunchableApp) {
        Log.d("PurposeAppLauncher", "Launching app ${app.packageName}")
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        val purpose = intent.getStringExtra(EXTRA_PURPOSE)
        lifecycleScope.launch {
            appOpenEventDao.insert(
                AppOpenEvent(
                    packageName = app.packageName,
                    appName = app.appName,
                    unlockPurpose = purpose
                )
            )
        }
        startActivity(launchIntent)
        finish()
    }
}

// ── Screen ──────────────────────────────────────────────────────────────────

@Composable
private fun PurposeAppLauncherScreen(
    purpose: String,
    apps: List<LaunchableApp>,
    isLoading: Boolean,
    onOpenApp: (LaunchableApp) -> Unit,
    onBack: () -> Unit
) {
    val isSearchMode = purpose == PURPOSE_SEARCH_APP
    var searchQuery by remember { mutableStateOf("") }

    val displayedApps = if (isSearchMode && searchQuery.isNotBlank()) {
        apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    } else if (isSearchMode) {
        emptyList()
    } else {
        apps
    }

    val title = when {
        isSearchMode -> "Find an app"
        else -> purpose
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E21),
                        Color(0xFF0D1B3E),
                        Color(0xFF080C1A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x14FFFFFF))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFB0BEC5),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!isSearchMode) {
                        Text(
                            text = "${displayedApps.size} apps matched",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7B8CB8)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Search field
            if (isSearchMode) {
                GlassSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Type app name..."
                )
                Spacer(Modifier.height(20.dp))
            }

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF3D5AFE),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                isSearchMode && searchQuery.isBlank() -> {
                    EmptyState(
                        icon = Icons.Outlined.Search,
                        message = "Start typing to find an app",
                        modifier = Modifier.weight(1f)
                    )
                }

                displayedApps.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Rounded.Apps,
                        message = if (isSearchMode) "No apps found for \"$searchQuery\""
                                  else "No apps matched this purpose",
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(displayedApps, key = { it.packageName }) { app ->
                            GlassAppIcon(
                                app = app,
                                onClick = { onOpenApp(app) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Bottom action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x14FFFFFF))
                    .border(
                        width = 1.dp,
                        color = Color(0x20FFFFFF),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(onClick = onBack)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Go to home screen",
                    color = Color(0xFF7B8CB8),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Glass Search Field ──────────────────────────────────────────────────────

@Composable
private fun GlassSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    val shape = RoundedCornerShape(16.dp)

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(placeholder, color = Color(0xFF5A6A8A))
        },
        leadingIcon = {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = Color(0xFF5A6A8A),
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF3D5AFE),
            unfocusedBorderColor = Color(0x30FFFFFF),
            cursorColor = Color(0xFF3D5AFE),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0x14FFFFFF),
            unfocusedContainerColor = Color(0x0AFFFFFF)
        )
    )
}

// ── Glass App Icon ──────────────────────────────────────────────────────────

@Composable
private fun GlassAppIcon(
    app: LaunchableApp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x14FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                update = { imageView -> imageView.setImageDrawable(app.icon) },
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = app.appName,
            color = Color(0xFFCCD6E8),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp
        )
    }
}

// ── Empty State ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF3A4460),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5A6A8A),
                textAlign = TextAlign.Center
            )
        }
    }
}
