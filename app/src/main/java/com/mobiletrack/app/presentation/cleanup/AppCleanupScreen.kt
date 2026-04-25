package com.mobiletrack.app.presentation.cleanup

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

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
private val AccentRed = Color(0xFFEF5350)
private val AccentAmber = Color(0xFFFFCA28)
private val AccentGreen = Color(0xFF66BB6A)
private val AccentPurple = Color(0xFF7C4DFF)

@Composable
fun AppCleanupScreen(
    navController: NavController,
    viewModel: AppCleanupViewModel = hiltViewModel()
) {
    val unusedApps by viewModel.unusedApps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val totalApps by viewModel.totalInstalledApps.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "App Cleanup",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 26.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Find and remove unused apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Summary hero metrics
            if (!isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HeroMetric(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Apps,
                            value = "$totalApps",
                            label = "Installed",
                            accentColor = AccentBlue
                        )
                        HeroMetric(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.DeleteSweep,
                            value = "${unusedApps.size}",
                            label = "Unused",
                            accentColor = AccentAmber
                        )
                        HeroMetric(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Storage,
                            value = formatSize(unusedApps.sumOf { it.sizeBytes }),
                            label = "Reclaimable",
                            accentColor = AccentPurple
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = AccentBlue,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Scanning installed apps...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else if (unusedApps.isEmpty()) {
                item {
                    Spacer(Modifier.height(60.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(AccentGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "All clean!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "No unused apps found. You're organized.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Section header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Unused Apps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "${unusedApps.size} apps",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                }

                items(unusedApps, key = { it.packageName }) { app ->
                    UnusedAppCard(
                        app = app,
                        onUninstall = {
                            val intent = Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.parse("package:${app.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ── Unused App Card ─────────────────────────────────────────────────────────

@Composable
private fun UnusedAppCard(
    app: UnusedAppInfo,
    onUninstall: () -> Unit
) {
    val staleDays = app.daysSinceLastUsed
    val staleColor = when {
        staleDays >= 90 -> AccentRed
        staleDays >= 30 -> AccentAmber
        else -> TextSecondary
    }

    val staleLabel = when {
        staleDays == -1 -> "Never used"
        staleDays >= 365 -> "${staleDays / 365}y ${(staleDays % 365) / 30}m ago"
        staleDays >= 30 -> "${staleDays / 30}m ago"
        staleDays >= 7 -> "${staleDays / 7}w ago"
        else -> "${staleDays}d ago"
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stale indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(staleColor)
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniTag(
                        icon = Icons.Outlined.Schedule,
                        text = staleLabel,
                        color = staleColor
                    )
                    if (app.sizeBytes > 0) {
                        MiniTag(
                            icon = Icons.Outlined.Storage,
                            text = formatSize(app.sizeBytes),
                            color = TextMuted
                        )
                    }
                }
            }

            // Uninstall button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentRed.copy(alpha = 0.12f))
                    .clickable(onClick = onUninstall)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Uninstall",
                        tint = AccentRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Remove",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentRed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniTag(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 11.sp)
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
private fun GlassIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.1fGB".format(gb)
        mb >= 1.0 -> "%.0fMB".format(mb)
        else -> "${bytes / 1024}KB"
    }
}
