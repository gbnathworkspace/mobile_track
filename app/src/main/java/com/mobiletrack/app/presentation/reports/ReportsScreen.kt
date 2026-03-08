package com.mobiletrack.app.presentation.reports

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
import com.mobiletrack.app.presentation.dashboard.formatMinutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val weeklyUsage by viewModel.weeklyUsage.collectAsStateWithLifecycle(emptyList())
    val weeklyUnlocks by viewModel.weeklyUnlocks.collectAsStateWithLifecycle(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("7-Day Report") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "Total Unlocks",
                        value = "${weeklyUnlocks ?: 0}",
                        icon = Icons.Default.Visibility
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "Days Tracked",
                        value = "${weeklyUsage.map { it.date }.distinct().size}",
                        icon = Icons.Default.CalendarToday
                    )
                }
            }

            item {
                Text("App Breakdown (7 days)", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }

            val grouped = weeklyUsage
                .groupBy { it.packageName }
                .map { (_, sessions) ->
                    sessions.first().copy(
                        totalMinutes = sessions.sumOf { it.totalMinutes }
                    )
                }
                .sortedByDescending { it.totalMinutes }

            items(grouped) { session ->
                WeeklyAppRow(session = session)
            }
        }
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun WeeklyAppRow(session: AppUsageSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(session.appName, style = MaterialTheme.typography.bodyLarge)
            Text(
                formatMinutes(session.totalMinutes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
