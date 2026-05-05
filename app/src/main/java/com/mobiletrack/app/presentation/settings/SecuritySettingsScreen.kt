package com.mobiletrack.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mobiletrack.app.data.local.dao.AppRuleDao
import com.mobiletrack.app.presentation.design.MTSpacing
import com.mobiletrack.app.presentation.design.components.MTCard
import com.mobiletrack.app.presentation.pin.PinKind
import com.mobiletrack.app.presentation.pin.PinSetHost
import com.mobiletrack.app.presentation.pin.userPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SecurityDaoEntryPoint {
    fun appRuleDao(): AppRuleDao
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(navController: NavController) {
    val prefs = userPreferences()
    val ctx = LocalContext.current.applicationContext
    val ruleDao = remember {
        EntryPointAccessors.fromApplication(ctx, SecurityDaoEntryPoint::class.java).appRuleDao()
    }
    val entryEnabled by prefs.appEntryPinEnabled.collectAsStateWithLifecycle(false)
    val lockEnabled by prefs.appLockPinEnabled.collectAsStateWithLifecycle(false)
    var pinSetting by remember { mutableStateOf<PinKind?>(null) }
    val scope = rememberCoroutineScope()

    if (pinSetting != null) {
        PinSetHost(kind = pinSetting!!, onDone = { pinSetting = null })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(MTSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MTSpacing.sm)
        ) {
            PinSection(
                title = "MobileTrack PIN",
                subtitle = "Require a PIN to open MobileTrack",
                enabled = entryEnabled,
                onSet = { pinSetting = PinKind.AppEntry },
                onClear = { scope.launch { prefs.clearAppEntryPin() } }
            )
            PinSection(
                title = "App-Lock PIN",
                subtitle = "PIN for unlocking apps you've App-Locked",
                enabled = lockEnabled,
                onSet = { pinSetting = PinKind.AppLock },
                onClear = {
                    scope.launch {
                        prefs.clearAppLockPin()
                        ruleDao.clearAllAppLocks()
                    }
                }
            )
        }
    }
}

@Composable
private fun PinSection(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onSet: () -> Unit,
    onClear: () -> Unit
) {
    MTCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MTSpacing.md)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onSet) {
                    Text(if (enabled) "Change PIN" else "Set PIN")
                }
                if (enabled) {
                    OutlinedButton(onClick = onClear) { Text("Disable") }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (enabled) "Enabled" else "Disabled",
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
