package com.mobiletrack.app.presentation.settings

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mobiletrack.app.presentation.design.MTIconSize
import com.mobiletrack.app.presentation.design.MTSpacing
import com.mobiletrack.app.presentation.design.components.MTCard
import com.mobiletrack.app.presentation.design.components.MTIconRow
import com.mobiletrack.app.presentation.design.components.MTSubsectionLabel
import com.mobiletrack.app.presentation.theme.UnlockTheme

fun isUsageAccessGranted(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(context.packageName)
}

fun isOverlayGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val selectedTheme by viewModel.unlockTheme.collectAsStateWithLifecycle()

    var usageGranted by remember { mutableStateOf(isUsageAccessGranted(context)) }
    var accessibilityGranted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var overlayGranted by remember { mutableStateOf(isOverlayGranted(context)) }

    // Re-check permissions every time screen resumes (user returning from Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageGranted = isUsageAccessGranted(context)
                accessibilityGranted = isAccessibilityServiceEnabled(context)
                overlayGranted = isOverlayGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(MTSpacing.md),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MTSubsectionLabel("Permissions")

            PermissionItem(
                icon = Icons.Default.QueryStats,
                title = "Usage Access",
                subtitle = "Required to track screen time",
                granted = usageGranted,
                onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            )

            PermissionItem(
                icon = Icons.Default.Accessibility,
                title = "Accessibility Service",
                subtitle = "Required to block apps and detect scrolling",
                granted = accessibilityGranted,
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )

            PermissionItem(
                icon = Icons.Default.Layers,
                title = "Display Over Other Apps",
                subtitle = "Required for unlock prompt overlay",
                granted = overlayGranted,
                onClick = {
                    context.startActivity(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ))
                }
            )

            HorizontalDivider()

            MTSubsectionLabel("Unlock Prompt Theme")

            UnlockThemeSelector(
                selected = selectedTheme,
                onSelect = { viewModel.setUnlockTheme(it) }
            )

            HorizontalDivider()

            MTSubsectionLabel("About")

            MTCard(modifier = Modifier.fillMaxWidth()) {
                MTIconRow(
                    icon = Icons.Default.Info,
                    title = "MobileTrack v1.0",
                    subtitle = "Privacy-first screen time control. No data leaves your device."
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    MTCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (granted)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(MTSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(MTIconSize.md))
            Spacer(Modifier.width(MTSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.width(MTSpacing.sm))
            if (granted) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(MTIconSize.md))
            } else {
                Icon(Icons.Default.Cancel, contentDescription = "Not granted",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(MTIconSize.md))
            }
        }
    }
}

@Composable
fun UnlockThemeSelector(
    selected: UnlockTheme,
    onSelect: (UnlockTheme) -> Unit
) {
    val options = listOf(
        UnlockTheme.LIGHT to "Light",
        UnlockTheme.DARK to "Dark",
        UnlockTheme.GLASS to "Glass"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (theme, label) ->
            val isSelected = selected == theme
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(theme) },
                label = { Text(label) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
