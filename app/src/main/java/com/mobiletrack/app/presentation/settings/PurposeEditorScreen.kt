package com.mobiletrack.app.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

// Default purpose definitions
data class PurposeDef(
    val label: String,
    val mapKey: String,
    val icon: ImageVector,
    val tint: Color,
    val defaultHints: List<String>,
    val alwaysOn: Boolean = false
)

val ALL_PURPOSE_DEFS = listOf(
    PurposeDef("Messages", "Check messages", Icons.Outlined.Chat, Color(0xFF64B5F6),
        listOf("whatsapp", "telegram", "signal", "messages", "instagram", "snapchat")),
    PurposeDef("Call", "Make a call", Icons.Outlined.Phone, Color(0xFF81C784),
        listOf("dialer", "phone", "contacts", "whatsapp", "telegram")),
    PurposeDef("Scan & Pay", "Scan & Pay", Icons.Outlined.QrCodeScanner, Color(0xFF66BB6A),
        listOf("gpay", "tez", "phonepe", "paytm", "bhim", "amazonpay", "cred", "scanner", "qr")),
    PurposeDef("Maps", "Navigation / Maps", Icons.Outlined.Place, Color(0xFFFF8A65),
        listOf("maps", "waze", "uber", "ola")),
    PurposeDef("Music", "Music / Podcast", Icons.Outlined.MusicNote, Color(0xFFBA68C8),
        listOf("spotify", "youtube", "music", "podcast", "gaana", "jiosaavn")),
    PurposeDef("Work", "Work / Email", Icons.Outlined.Work, Color(0xFF4FC3F7),
        listOf("gmail", "outlook", "slack", "teams", "notion", "drive")),
    PurposeDef("Read", "Read / Books", Icons.Outlined.MenuBook, Color(0xFFCE93D8),
        listOf("kindle", "play.books", "moon.reader", "kobo", "libby", "audible", "pocket")),
    PurposeDef("Camera", "Camera", Icons.Outlined.CameraAlt, Color(0xFFFFD54F),
        listOf("camera")),
    PurposeDef("Alarm", "Alarm / Timer", Icons.Outlined.Alarm, Color(0xFFE57373),
        listOf("clock", "alarm", "timer")),
    PurposeDef("Calendar", "Calendar / Tasks", Icons.Outlined.CalendarMonth, Color(0xFF4DB6AC),
        listOf("calendar", "tasks", "todoist", "keep")),
    PurposeDef("Finance", "Stocks / Finance", Icons.Outlined.TrendingUp, Color(0xFFA1887F),
        listOf("kite", "zerodha", "tickertape", "groww", "upstox", "angelone", "mstock")),
    PurposeDef("Search App", "Search App", Icons.Outlined.Search, Color(0xFF90A4AE),
        emptyList(), alwaysOn = true),
)

@Composable
fun PurposeEditorScreen(
    navController: NavController,
    viewModel: PurposeEditorViewModel = hiltViewModel()
) {
    val disabledPurposes by viewModel.disabledPurposes.collectAsStateWithLifecycle(emptySet())
    val customHints by viewModel.customHintsMap.collectAsStateWithLifecycle(emptyMap())
    var expandedLabel by remember { mutableStateOf<String?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            "Purpose Prompt",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 26.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Choose categories & app keywords",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            // Purpose cards
            items(ALL_PURPOSE_DEFS) { def ->
                val isEnabled = def.alwaysOn || def.label !in disabledPurposes
                val isExpanded = expandedLabel == def.label
                val hints = customHints[def.mapKey] ?: def.defaultHints

                PurposeEditorCard(
                    def = def,
                    enabled = isEnabled,
                    expanded = isExpanded,
                    hints = hints,
                    onToggle = {
                        if (!def.alwaysOn) viewModel.togglePurpose(def.label, disabledPurposes)
                    },
                    onExpandToggle = {
                        expandedLabel = if (isExpanded) null else def.label
                    },
                    onAddHint = { hint ->
                        viewModel.addHint(def.mapKey, hint, customHints)
                    },
                    onRemoveHint = { hint ->
                        viewModel.removeHint(def.mapKey, hint, customHints)
                    }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun PurposeEditorCard(
    def: PurposeDef,
    enabled: Boolean,
    expanded: Boolean,
    hints: List<String>,
    onToggle: () -> Unit,
    onExpandToggle: () -> Unit,
    onAddHint: (String) -> Unit,
    onRemoveHint: (String) -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(def.tint.copy(alpha = 0.12f * alpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        def.icon,
                        contentDescription = null,
                        tint = def.tint.copy(alpha = alpha),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        def.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary.copy(alpha = alpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${hints.size} app keywords",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                // Expand button (only for non-search purposes)
                if (!def.alwaysOn || def.defaultHints.isNotEmpty()) {
                    GlassIconButton(
                        onClick = onExpandToggle,
                        size = 32.dp
                    ) {
                        Icon(
                            if (expanded) Icons.Rounded.KeyboardArrowUp
                            else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }

                // Toggle
                if (!def.alwaysOn) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0x14FFFFFF)
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            // Expanded: app keywords
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Keyword chips
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        hints.forEach { hint ->
                            KeywordChip(
                                text = hint,
                                onRemove = { onRemoveHint(hint) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Add keyword input
                    AddKeywordRow(onAdd = onAddHint)
                }
            }
        }
    }
}

@Composable
private fun KeywordChip(text: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x18FFFFFF))
            .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(10.dp))
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 11.sp
        )
        Spacer(Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = TextMuted,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun AddKeywordRow(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Add keyword...", color = TextMuted, fontSize = 12.sp) },
            modifier = Modifier.weight(1f).height(44.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = Color(0x15FFFFFF),
                cursorColor = AccentBlue,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        Spacer(Modifier.width(8.dp))
        GlassIconButton(
            onClick = {
                val trimmed = text.trim().lowercase()
                if (trimmed.isNotEmpty()) {
                    onAdd(trimmed)
                    text = ""
                }
            },
            size = 36.dp
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = AccentBlue, modifier = Modifier.size(16.dp))
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
