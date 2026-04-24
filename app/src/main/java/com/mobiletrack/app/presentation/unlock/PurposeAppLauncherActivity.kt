package com.mobiletrack.app.presentation.unlock

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.mobiletrack.app.data.local.dao.AppOpenEventDao
import com.mobiletrack.app.data.local.entity.AppOpenEvent
import com.mobiletrack.app.presentation.theme.MobileTrackTheme
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LaunchableApp(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)

@dagger.hilt.android.AndroidEntryPoint
class PurposeAppLauncherActivity : ComponentActivity() {

    @Inject lateinit var appOpenEventDao: AppOpenEventDao

    private var apps by mutableStateOf<List<LaunchableApp>>(emptyList())
    private var isLoading by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val purpose = intent.getStringExtra(EXTRA_PURPOSE).orEmpty()
        Log.d("PurposeAppLauncher", "onCreate purpose=$purpose")

        setContent {
            MobileTrackTheme {
                PurposeAppLauncherScreen(
                    purpose = purpose,
                    apps = apps,
                    isLoading = isLoading,
                    onOpenApp = ::openApp,
                    onOpenAllApps = ::finish
                )
            }
        }

        lifecycleScope.launch {
            val allApps = withContext(Dispatchers.IO) { loadInstalledApps() }
            apps = if (purpose == PURPOSE_SEARCH_APP) {
                allApps // show all, filtering happens via search query in UI
            } else {
                allApps.filter { app -> matchesPurpose(app.packageName, purpose) }
            }
            isLoading = false
        }
    }

    private fun loadInstalledApps(): List<LaunchableApp> {
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager.queryIntentActivities(launchIntent, 0)
            .map { resolveInfo ->
                LaunchableApp(
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
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

@Composable
private fun PurposeAppLauncherScreen(
    purpose: String,
    apps: List<LaunchableApp>,
    isLoading: Boolean,
    onOpenApp: (LaunchableApp) -> Unit,
    onOpenAllApps: () -> Unit
) {
    val isSearchMode = purpose == PURPOSE_SEARCH_APP
    var searchQuery by remember { mutableStateOf("") }

    val displayedApps = if (isSearchMode && searchQuery.isNotBlank()) {
        apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    } else if (isSearchMode) {
        emptyList() // show nothing until user types
    } else {
        apps
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isSearchMode) "Search for an app" else purpose,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            if (isSearchMode) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Type app name…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(16.dp))
            } else {
                Text(
                    text = "Only apps matching this purpose are shown.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                isSearchMode && searchQuery.isBlank() -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Start typing to find an app.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                displayedApps.isEmpty() -> {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isSearchMode) "No apps found for \"$searchQuery\"."
                                   else "No installed apps matched this purpose.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayedApps, key = { it.packageName }) { app ->
                            AppLauncherCard(app = app, onClick = { onOpenApp(app) })
                        }
                    }
                }
            }

            Button(
                onClick = onOpenAllApps,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Open all apps")
            }
        }
    }
}

@Composable
private fun AppLauncherCard(
    app: LaunchableApp,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                update = { imageView ->
                    imageView.setImageDrawable(app.icon)
                },
                modifier = Modifier.size(48.dp)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
