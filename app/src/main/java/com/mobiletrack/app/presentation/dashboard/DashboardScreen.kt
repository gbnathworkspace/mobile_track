package com.mobiletrack.app.presentation.dashboard

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
import com.mobiletrack.app.data.local.entity.AppUsageSession
import com.mobiletrack.app.presentation.design.MTIconSize
import com.mobiletrack.app.presentation.design.MTSpacing
import com.mobiletrack.app.presentation.design.components.MTCard
import com.mobiletrack.app.presentation.design.components.MTMetricCard
import com.mobiletrack.app.presentation.design.components.MTSectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val totalMinutes by viewModel.totalMinutesToday.collectAsStateWithLifecycle(0)
    val unlockCount by viewModel.unlockCountToday.collectAsStateWithLifecycle(0)
    val topApps by viewModel.topAppsToday.collectAsStateWithLifecycle(emptyList())
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Overview") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(MTSpacing.md),
            verticalArrangement = Arrangement.spacedBy(MTSpacing.md)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MTMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PhoneAndroid,
                        label = "Screen Time",
                        value = formatMinutes(totalMinutes ?: 0),
                        color = MaterialTheme.colorScheme.primary
                    )
                    MTMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Visibility,
                        label = "Unlocks",
                        value = "${unlockCount ?: 0}",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            item {
                StreakCard(days = streakDays ?: 0)
            }

            item {
                MTSectionTitle("Top Apps Today")
            }

            items(topApps.take(5)) { session ->
                AppUsageRow(session = session)
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun StreakCard(days: Int) {
    MTCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(MTSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = "Streak",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(MTIconSize.xl)
            )
            Spacer(Modifier.width(MTSpacing.md))
            Column {
                Text(
                    "$days day streak",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Keep staying within your limits!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun AppUsageRow(session: AppUsageSession) {
    MTCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MTSpacing.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.appName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${session.openCount} opens today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                formatMinutes(session.totalMinutes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
