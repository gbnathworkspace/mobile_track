package com.mobiletrack.app.presentation.applimits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mobiletrack.app.data.local.entity.AppRule
import com.mobiletrack.app.presentation.design.MTSpacing
import com.mobiletrack.app.presentation.design.components.MTCard
import com.mobiletrack.app.presentation.design.components.MTEmptyState
import com.mobiletrack.app.presentation.design.components.MTSearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLimitsScreen(
    navController: NavController,
    viewModel: AppLimitsViewModel = hiltViewModel()
) {
    val rules by viewModel.allRules.collectAsStateWithLifecycle(emptyList())
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Limits") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add limit")
            }
        }
    ) { padding ->
        if (rules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                MTEmptyState(
                    icon = Icons.Default.Timer,
                    title = "No limits set",
                    subtitle = "Tap + to add a time limit for an app"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(MTSpacing.md),
                verticalArrangement = Arrangement.spacedBy(MTSpacing.sm)
            ) {
                items(rules) { rule ->
                    AppRuleCard(
                        rule = rule,
                        onToggleBlock = { viewModel.toggleBlock(rule) },
                        onDelete = { viewModel.deleteRule(rule.packageName) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddAppLimitDialog(
            installedApps = installedApps,
            onDismiss = { showAddDialog = false },
            onConfirm = { pkg, appName, limitMinutes, scrollLimit ->
                viewModel.setLimit(pkg, appName, limitMinutes, scrollLimit)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AppRuleCard(
    rule: AppRule,
    onToggleBlock: () -> Unit,
    onDelete: () -> Unit
) {
    MTCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(MTSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.appName, fontWeight = FontWeight.SemiBold)
                if (rule.dailyLimitMinutes > 0) {
                    Text(
                        "Time: ${rule.dailyLimitMinutes}m/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (rule.dailyScrollLimit > 0) {
                    Text(
                        "Scrolls: ${rule.dailyScrollLimit}/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (rule.isBlocked) {
                    Text(
                        "BLOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            IconButton(onClick = onToggleBlock) {
                Icon(
                    if (rule.isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                    contentDescription = if (rule.isBlocked) "Unblock" else "Block",
                    tint = if (rule.isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun AddAppLimitDialog(
    installedApps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onConfirm: (packageName: String, appName: String, limitMinutes: Int, scrollLimit: Int) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var limitMinutes by remember { mutableStateOf("60") }
    var scrollLimit by remember { mutableStateOf("") }

    val filtered = remember(search, installedApps) {
        if (search.isBlank()) installedApps
        else installedApps.filter { it.appName.contains(search, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedApp == null) "Select App" else "Set Limit") },
        text = {
            if (selectedApp == null) {
                Column {
                    MTSearchField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Search apps..."
                    )
                    Spacer(Modifier.height(MTSpacing.sm))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filtered) { app ->
                            ListItem(
                                headlineContent = { Text(app.appName) },
                                supportingContent = {
                                    Text(app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline)
                                },
                                modifier = Modifier.clickable { selectedApp = app }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(MTSpacing.sm)) {
                    Text(selectedApp!!.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(selectedApp!!.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(MTSpacing.xs))
                    OutlinedTextField(
                        value = limitMinutes,
                        onValueChange = { limitMinutes = it },
                        label = { Text("Daily Time Limit (minutes, 0 = none)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = scrollLimit,
                        onValueChange = { scrollLimit = it },
                        label = { Text("Daily Scroll Limit (0 = none)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            if (selectedApp != null) {
                Button(onClick = {
                    val minutes = limitMinutes.toIntOrNull() ?: 60
                    val scrolls = scrollLimit.toIntOrNull() ?: 0
                    onConfirm(selectedApp!!.packageName, selectedApp!!.appName, minutes, scrolls)
                }) { Text("Save") }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (selectedApp != null) selectedApp = null else onDismiss()
            }) { Text(if (selectedApp != null) "Back" else "Cancel") }
        }
    )
}
