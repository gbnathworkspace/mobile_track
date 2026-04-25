package com.mobiletrack.app.presentation.applimits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mobiletrack.app.data.local.entity.AppRule

// ── Colors (consistent with Dashboard/Reports) ──────────────────────────────
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
private val AccentGreen = Color(0xFF66BB6A)
private val AccentAmber = Color(0xFFFFCA28)

@Composable
fun AppLimitsScreen(
    navController: NavController,
    viewModel: AppLimitsViewModel = hiltViewModel()
) {
    val rules by viewModel.allRules.collectAsStateWithLifecycle(emptyList())
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
                        Text(
                            "App Limits",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 26.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    GlassIconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add limit",
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (rules.isEmpty()) {
                item {
                    Spacer(Modifier.height(80.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "No limits set",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap + to add a time limit for an app",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Active Limits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "${rules.size} apps",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                }
                items(rules) { rule ->
                    AppRuleCard(
                        rule = rule,
                        onToggleBlock = { viewModel.toggleBlock(rule) },
                        onDelete = { viewModel.deleteRule(rule.packageName) }
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
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
private fun AppRuleCard(
    rule: AppRule,
    onToggleBlock: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when {
        rule.isBlocked -> AccentRed
        rule.dailyLimitMinutes > 0 -> AccentAmber
        else -> AccentGreen
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        rule.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (rule.dailyLimitMinutes > 0) {
                            MiniTag(
                                icon = Icons.Outlined.Timer,
                                text = "${rule.dailyLimitMinutes}m/day",
                                color = AccentBlue
                            )
                        }
                        if (rule.dailyScrollLimit > 0) {
                            MiniTag(
                                icon = Icons.Outlined.SwipeVertical,
                                text = "${rule.dailyScrollLimit} scrolls",
                                color = AccentAmber
                            )
                        }
                        if (rule.isBlocked) {
                            MiniTag(
                                icon = Icons.Outlined.Block,
                                text = "Blocked",
                                color = AccentRed
                            )
                        }
                    }
                }

                // Action buttons
                Row {
                    GlassIconButton(
                        onClick = onToggleBlock,
                        size = 36.dp
                    ) {
                        Icon(
                            if (rule.isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                            contentDescription = if (rule.isBlocked) "Unblock" else "Block",
                            tint = if (rule.isBlocked) AccentGreen else AccentRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(
                        onClick = onDelete,
                        size = 36.dp
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniTag(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 11.sp)
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
        containerColor = Color(0xFF131A2E),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                if (selectedApp == null) "Select App" else "Set Limit",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (selectedApp == null) {
                Column {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search apps...", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = Color(0x20FFFFFF),
                            cursorColor = AccentBlue,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filtered) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedApp = app }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        app.appName,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0x10FFFFFF))
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        selectedApp!!.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        selectedApp!!.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    OutlinedTextField(
                        value = limitMinutes,
                        onValueChange = { limitMinutes = it },
                        label = { Text("Daily Time Limit (minutes)", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = Color(0x20FFFFFF),
                            cursorColor = AccentBlue,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = AccentBlue
                        )
                    )
                    OutlinedTextField(
                        value = scrollLimit,
                        onValueChange = { scrollLimit = it },
                        label = { Text("Daily Scroll Limit (0 = none)", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = Color(0x20FFFFFF),
                            cursorColor = AccentBlue,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = AccentBlue
                        )
                    )
                }
            }
        },
        confirmButton = {
            if (selectedApp != null) {
                Button(
                    onClick = {
                        val minutes = limitMinutes.toIntOrNull() ?: 60
                        val scrolls = scrollLimit.toIntOrNull() ?: 0
                        onConfirm(selectedApp!!.packageName, selectedApp!!.appName, minutes, scrolls)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save", color = Color.White) }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (selectedApp != null) selectedApp = null else onDismiss()
            }) {
                Text(
                    if (selectedApp != null) "Back" else "Cancel",
                    color = TextSecondary
                )
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
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
    size: androidx.compose.ui.unit.Dp = 40.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
