package com.mobiletrack.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mobiletrack.app.data.local.dao.HourlyCount
import com.mobiletrack.app.data.local.entity.AppRule
import com.mobiletrack.app.data.local.entity.AppUsageSession
import java.util.Calendar

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
private val AccentGreen = Color(0xFF66BB6A)
private val AccentAmber = Color(0xFFFFCA28)
private val AccentRed = Color(0xFFEF5350)
private val AccentPurple = Color(0xFF7C4DFF)

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val totalMinutes by viewModel.totalMinutesToday.collectAsStateWithLifecycle(initialValue = 0)
    val unlockCount by viewModel.unlockCountToday.collectAsStateWithLifecycle(initialValue = 0)
    val topApps by viewModel.topAppsToday.collectAsStateWithLifecycle(initialValue = emptyList())
    val allRules by viewModel.allRules.collectAsStateWithLifecycle(initialValue = emptyList())
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle(initialValue = 0)
    val hourlyBreakdown by viewModel.hourlyBreakdown.collectAsStateWithLifecycle(initialValue = emptyList())

    val rulesMap = remember(allRules) { allRules.associateBy { it.packageName } }
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Today",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 30.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            getGreeting(hour),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBg)
                            .clickable { navController.navigate("settings") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Hero metrics row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.PhoneAndroid,
                        value = formatMinutes(totalMinutes),
                        label = "Screen Time",
                        accentColor = AccentBlue
                    )
                    HeroMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Visibility,
                        value = "$unlockCount",
                        label = "Unlocks",
                        accentColor = AccentPurple
                    )
                    if (streakDays > 0) {
                        HeroMetric(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.LocalFireDepartment,
                            value = "${streakDays}d",
                            label = "Streak",
                            accentColor = Color(0xFFFF8A65)
                        )
                    }
                }
            }

            // Peak hours chart
            if (hourlyBreakdown.isNotEmpty()) {
                item {
                    GlassSection(title = "Activity Today") {
                        PeakHoursChart(hourlyBreakdown = hourlyBreakdown)
                    }
                }
            }

            // App usage list
            if (topApps.isNotEmpty()) {
                item {
                    SectionHeader(title = "App Usage", count = topApps.size)
                }

                items(topApps) { session ->
                    val rule = rulesMap[session.packageName]
                    AppUsageCard(session = session, rule = rule)
                }
            }
        }
    }
}

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
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    letterSpacing = (-0.3).sp
                ),
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

// ── App Usage Card with progress bar ────────────────────────────────────────

@Composable
private fun AppUsageCard(session: AppUsageSession, rule: AppRule?) {
    val hasLimit = rule != null && rule.dailyLimitMinutes > 0
    val limitMinutes = rule?.dailyLimitMinutes ?: 0
    val progress = if (hasLimit) (session.totalMinutes.toFloat() / limitMinutes).coerceIn(0f, 1f) else -1f
    val isOverLimit = hasLimit && session.totalMinutes >= limitMinutes

    val progressColor = when {
        isOverLimit -> AccentRed
        progress > 0.7f -> AccentAmber
        progress >= 0f -> AccentGreen
        else -> AccentBlue
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniStat(
                            icon = Icons.Outlined.TouchApp,
                            value = "${session.openCount} opens",
                        )
                        if (session.totalScrolls > 0) {
                            MiniStat(
                                icon = Icons.Outlined.SwipeVertical,
                                value = "${session.totalScrolls} scrolls",
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatMinutes(session.totalMinutes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    if (hasLimit) {
                        Text(
                            "/ ${formatMinutes(limitMinutes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Progress bar
            if (hasLimit) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x14FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(progressColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

// ── Peak Hours Chart ────────────────────────────────────────────────────────

@Composable
private fun PeakHoursChart(hourlyBreakdown: List<HourlyCount>) {
    val maxCount = hourlyBreakdown.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (hour in 0..23) {
                val count = hourlyBreakdown.find { it.hourOfDay == hour }?.count ?: 0
                val fraction = count.toFloat() / maxCount
                val isCurrent = hour == currentHour
                val barColor = when {
                    isCurrent -> AccentBlue
                    count > 0 -> AccentBlue.copy(alpha = 0.4f)
                    else -> Color(0x10FFFFFF)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction.coerceAtLeast(0.06f))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(barColor)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("12a", "6a", "12p", "6p").forEachIndexed { index, label ->
                if (index > 0) Spacer(Modifier.weight(6f))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
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
    ) {
        content()
    }
}

@Composable
private fun GlassSection(
    title: String,
    content: @Composable () -> Unit
) {
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

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            "$count apps",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted
        )
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun getGreeting(hour: Int): String = when {
    hour < 6 -> "It's late — rest well"
    hour < 12 -> "Start your day with intention"
    hour < 17 -> "Stay focused this afternoon"
    hour < 21 -> "Wind down mindfully"
    else -> "Screen off soon — you've got this"
}

fun formatMinutes(minutes: Int): String {
    val clamped = minutes.coerceAtLeast(0)
    val h = clamped / 60
    val m = clamped % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
