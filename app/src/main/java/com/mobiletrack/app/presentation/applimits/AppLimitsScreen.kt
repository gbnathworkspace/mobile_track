package com.mobiletrack.app.presentation.applimits

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLimitsScreen(
    navController: NavController,
    viewModel: AppLimitsViewModel = hiltViewModel()
) {
    val rules by viewModel.allRules.collectAsStateWithLifecycle(emptyList())
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text("No limits set", style = MaterialTheme.typography.titleMedium)
                    Text("Tap + to add a time limit for an app", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
            onDismiss = { showAddDialog = false },
            onConfirm = { pkg, appName, limitMinutes ->
                viewModel.setLimit(pkg, appName, limitMinutes)
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.appName, fontWeight = FontWeight.SemiBold)
                if (rule.dailyLimitMinutes > 0) {
                    Text(
                        "Limit: ${rule.dailyLimitMinutes}m/day",
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
    onDismiss: () -> Unit,
    onConfirm: (packageName: String, appName: String, limitMinutes: Int) -> Unit
) {
    var packageName by remember { mutableStateOf("") }
    var appName by remember { mutableStateOf("") }
    var limitMinutes by remember { mutableStateOf("60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add App Limit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name (e.g. com.instagram.android)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = limitMinutes,
                    onValueChange = { limitMinutes = it },
                    label = { Text("Daily Limit (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val minutes = limitMinutes.toIntOrNull() ?: 60
                if (packageName.isNotBlank() && appName.isNotBlank()) {
                    onConfirm(packageName, appName, minutes)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
