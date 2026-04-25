package com.mobiletrack.app.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.text.style.TextOverflow
import com.mobiletrack.app.data.local.dao.AppOpenCount
import com.mobiletrack.app.data.local.entity.AppUsageSession
import com.mobiletrack.app.presentation.dashboard.formatMinutes
import java.text.SimpleDateFormat
import java.util.*

// ── Colors ──────────────────────────────────────────────────────────────────

private val BgGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0A0E21), Color(0xFF0D1B3E), Color(0xFF080C1A))
)
private val CardBg = Color(0x14FFFFFF)
private val CardBorder = Brush.linearGradient(listOf(Color(0x20FFFFFF), Color(0x08FFFFFF)))
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF7B8CB8)
private val TextMuted = Color(0xFF4A5A78)
private val AccentBlue = Color(0xFF3D5AFE)
private val AccentPurple = Color(0xFF7C4DFF)
private val AccentTeal = Color(0xFF26A69A)
private val AccentAmber = Color(0xFFFFCA28)
private val AccentGreen = Color(0xFF66BB6A)

@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val weeklyUsage by viewModel.weeklyUsage.collectAsStateWithLifecycle(emptyList())
    val weeklyUnlocks by viewModel.weeklyUnlocks.collectAsStateWithLifecycle(0)
    val weeklyOpenCounts by viewModel.weeklyOpenCounts.collectAsStateWithLifecycle(emptyList())

    // Computed data
    val daysTracked = remember(weeklyUsage) { weeklyUsage.map { it.date }.distinct().size }
    val totalMinutes = remember(weeklyUsage) { weeklyUsage.sumOf { it.totalMinutes } }
    val dailyAvg = remember(totalMinutes, daysTracked) {
        if (daysTracked > 0) totalMinutes / daysTracked else 0
    }

    // Daily totals for 7-day trend chart
    val dailyTotals = remember(weeklyUsage) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        val last7 = (6 downTo 0).map { offset ->
            cal.timeInMillis = System.currentTimeMillis() - offset * 24 * 60 * 60 * 1000L
            val dateStr = dateFormat.format(cal.time)
            val dayName = dayNameFormat.format(cal.time)
            val isToday = offset == 0
            DailyTotal(
                date = dateStr,
                dayLabel = if (isToday) "Today" else dayName,
                minutes = 0,
                isToday = isToday
            )
        }
        val minutesByDate = weeklyUsage.groupBy { it.date }
            .mapValues { (_, sessions) -> sessions.sumOf { it.totalMinutes } }
        last7.map { day -> day.copy(minutes = minutesByDate[day.date] ?: 0) }
    }

    // Top apps — merged time + opens into single list
    val topApps = remember(weeklyUsage, weeklyOpenCounts) {
        val openMap = weeklyOpenCounts.associate { it.packageName to it.openCount }
        weeklyUsage
            .groupBy { it.packageName }
            .map { (pkg, sessions) ->
                TopAppData(
                    appName = sessions.first().appName,
                    packageName = pkg,
                    totalMinutes = sessions.sumOf { it.totalMinutes },
                    totalOpens = openMap[pkg] ?: sessions.sumOf { it.openCount },
                    totalScrolls = sessions.sumOf { it.totalScrolls },
                    daysUsed = sessions.map { it.date }.distinct().size
                )
            }
            .sortedByDescending { it.totalMinutes }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGradient)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Text(
                    "This Week",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 30.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Hero metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.PhoneAndroid,
                        value = formatMinutes(totalMinutes),
                        label = "Total",
                        accentColor = AccentBlue
                    )
                    HeroMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.TrendingUp,
                        value = formatMinutes(dailyAvg),
                        label = "Daily Avg",
                        accentColor = AccentTeal
                    )
                    HeroMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Visibility,
                        value = "$weeklyUnlocks",
                        label = "Unlocks",
                        accentColor = AccentPurple
                    )
                }
            }

            // 7-day trend chart
            if (dailyTotals.any { it.minutes > 0 }) {
                item {
                    GlassSection(title = "Daily Trend") {
                        DailyTrendChart(dailyTotals = dailyTotals)
                    }
                }
            }

            // Top apps
            if (topApps.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Top Apps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "${topApps.size} apps",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                }
                items(topApps.take(10)) { app ->
                    TopAppCard(
                        app = app,
                        maxMinutes = topApps.first().totalMinutes
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ── Data classes ────────────────────────────────────────────────────────────

private data class DailyTotal(
    val date: String,
    val dayLabel: String,
    val minutes: Int,
    val isToday: Boolean
)

private data class TopAppData(
    val appName: String,
    val packageName: String,
    val totalMinutes: Int,
    val totalOpens: Int,
    val totalScrolls: Int,
    val daysUsed: Int
)

// ── Hero Metric Card ────────────────────────────────────────────────────────

@Composable
private fun HeroMetric(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, letterSpacing = (-0.3).sp),
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

// ── Daily Trend Chart (7-day bar chart) ─────────────────────────────────────

@Composable
private fun DailyTrendChart(dailyTotals: List<DailyTotal>) {
    val maxMinutes = dailyTotals.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dailyTotals.forEach { day ->
                val fraction = day.minutes.toFloat() / maxMinutes
                val barColor = when {
                    day.isToday -> AccentBlue
                    day.minutes > 0 -> AccentBlue.copy(alpha = 0.4f)
                    else -> Color(0x10FFFFFF)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Time label above bar
                    if (day.minutes > 0) {
                        Text(
                            formatMinutes(day.minutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (day.isToday) AccentBlue else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .fillMaxHeight(fraction.coerceAtLeast(0.04f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dailyTotals.forEach { day ->
                Text(
                    day.dayLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) TextPrimary else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// ── Top App Card (merged time + opens + scrolls) ────────────────────────────

@Composable
private fun TopAppCard(app: TopAppData, maxMinutes: Int) {
    val fraction = if (maxMinutes > 0) app.totalMinutes.toFloat() / maxMinutes else 0f

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniStat(
                            icon = Icons.Outlined.TouchApp,
                            text = "${app.totalOpens} opens",
                            color = AccentAmber
                        )
                        if (app.totalScrolls > 0) {
                            MiniStat(
                                icon = Icons.Outlined.SwipeVertical,
                                text = "${app.totalScrolls} scrolls",
                                color = AccentPurple
                            )
                        }
                        MiniStat(
                            icon = Icons.Outlined.CalendarMonth,
                            text = "${app.daysUsed}d",
                            color = AccentTeal
                        )
                    }
                }
                Text(
                    formatMinutes(app.totalMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
            }
            Spacer(Modifier.height(10.dp))
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x10FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentBlue.copy(alpha = 0.6f))
                )
            }
        }
    }
}

@Composable
private fun MiniStat(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 11.sp)
    }
}

// ── Glass components ────────────────────────────────────────────────────────

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(CardBg)
            .border(width = 1.dp, brush = CardBorder, shape = shape)
    ) { content() }
}

@Composable
private fun GlassSection(title: String, content: @Composable () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}
