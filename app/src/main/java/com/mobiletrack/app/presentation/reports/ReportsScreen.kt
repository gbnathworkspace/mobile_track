package com.mobiletrack.app.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mobiletrack.app.data.local.dao.AppOpenCount
import com.mobiletrack.app.data.local.dao.HourlyCount
import com.mobiletrack.app.data.local.entity.AppUsageSession
import com.mobiletrack.app.presentation.dashboard.formatMinutes
import com.mobiletrack.app.presentation.design.MTSpacing
import com.mobiletrack.app.presentation.design.components.MTCard
import com.mobiletrack.app.presentation.design.components.MTListValueRow
import com.mobiletrack.app.presentation.design.components.MTMetricCard
import com.mobiletrack.app.presentation.design.components.MTSectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val weeklyUsage by viewModel.weeklyUsage.collectAsStateWithLifecycle(emptyList())
    val weeklyUnlocks by viewModel.weeklyUnlocks.collectAsStateWithLifecycle(0)
    val weeklyOpenCounts by viewModel.weeklyOpenCounts.collectAsStateWithLifecycle(emptyList())
    val hourlyBreakdown by viewModel.hourlyBreakdown.collectAsStateWithLifecycle(emptyList())

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
                        label = "Total Unlocks",
                        value = "${weeklyUnlocks ?: 0}",
                        icon = Icons.Default.Visibility
                    )
                    MTMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Days Tracked",
                        value = "${weeklyUsage.map { it.date }.distinct().size}",
                        icon = Icons.Default.CalendarToday
                    )
                }
            }

            item {
                MTSectionTitle("App Breakdown (7 days)")
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

            if (weeklyOpenCounts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(MTSpacing.xs))
                    MTSectionTitle("Most Opened Apps (7 days)")
                }
                items(weeklyOpenCounts.take(10)) { entry ->
                    MostOpenedRow(entry = entry)
                }
            }

            if (hourlyBreakdown.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(MTSpacing.xs))
                    MTSectionTitle("Peak Hours")
                    Spacer(Modifier.height(MTSpacing.sm))
                    PeakHoursChart(hourlyBreakdown = hourlyBreakdown)
                }
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
    MTListValueRow(
        title = session.appName,
        value = formatMinutes(session.totalMinutes)
    )
}

@Composable
fun MostOpenedRow(entry: AppOpenCount) {
    MTListValueRow(
        title = entry.appName,
        value = "${entry.openCount}x",
        valueColor = MaterialTheme.colorScheme.secondary
    )
}

@Composable
fun PeakHoursChart(hourlyBreakdown: List<HourlyCount>) {
    val maxCount = hourlyBreakdown.maxOf { it.count }.coerceAtLeast(1)
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    MTCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MTSpacing.md)) {
            // Bar chart — one bar per hour present in data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (hour in 0..23) {
                    val count = hourlyBreakdown.find { it.hourOfDay == hour }?.count ?: 0
                    val fraction = count.toFloat() / maxCount
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction.coerceAtLeast(0.04f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (count > 0) primary else surfaceVariant)
                    )
                }
            }
            Spacer(Modifier.height(MTSpacing.xs))
            // Hour labels — show every 6 hours
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("12a", "6a", "12p", "6p").forEachIndexed { index, label ->
                    if (index > 0) Spacer(Modifier.weight((6f)))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
