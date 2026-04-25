package com.mobiletrack.app.presentation.unlock

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mobiletrack.app.presentation.theme.UnlockTheme
import com.mobiletrack.app.presentation.theme.UnlockPromptTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Colors (consistent with all screens) ────────────────────────────────────
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

@AndroidEntryPoint
class UnlockPromptActivity : ComponentActivity() {

    private val viewModel: UnlockPromptViewModel by viewModels()
    private var purposeSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("UnlockPromptActivity", "onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no-op */ }
        })

        setContent {
            val theme by viewModel.unlockTheme.collectAsStateWithLifecycle(UnlockTheme.GLASS)

            val disabledPurposes by viewModel.disabledPurposes.collectAsStateWithLifecycle(emptySet())

            UnlockPromptTheme(theme = theme) {
                UnlockPromptScreen(
                    disabledPurposes = disabledPurposes,
                    onPurposeSelected = { purpose ->
                        purposeSelected = true
                        lifecycleScope.launch {
                            if (purpose == "Scan & Pay") {
                                val scanIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tez://upi/scan")).apply {
                                    setPackage("com.google.android.apps.nbu.paisa.user")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    startActivity(scanIntent)
                                } catch (_: Exception) {
                                    val fallback = Intent(
                                        this@UnlockPromptActivity,
                                        PurposeAppLauncherActivity::class.java
                                    ).apply {
                                        putExtra(EXTRA_PURPOSE, purpose)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                    }
                                    startActivity(fallback)
                                }
                                viewModel.recordPurpose(purpose)
                                finish()
                                return@launch
                            }

                            val intent = Intent(
                                this@UnlockPromptActivity,
                                PurposeAppLauncherActivity::class.java
                            ).apply {
                                putExtra(EXTRA_PURPOSE, purpose)
                                addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                )
                            }
                            startActivity(intent)
                            viewModel.recordPurpose(purpose)
                            finish()
                        }
                    },
                    onDismiss = {
                        purposeSelected = true
                        lifecycleScope.launch {
                            viewModel.recordNoPurpose()
                            finish()
                        }
                    }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!purposeSelected && !isFinishing) {
            val relaunchIntent = Intent(this, UnlockPromptActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(relaunchIntent)
        }
    }
}

// ── Purpose model ───────────────────────────────────────────────────────────

private data class Purpose(
    val label: String,
    val icon: ImageVector,
    val tint: Color
)

const val PURPOSE_SEARCH_APP = "Search App"

private val PURPOSES = listOf(
    Purpose("Messages",       Icons.Outlined.Chat,          Color(0xFF64B5F6)),
    Purpose("Call",           Icons.Outlined.Phone,         Color(0xFF81C784)),
    Purpose("Scan & Pay",     Icons.Outlined.QrCodeScanner, Color(0xFF66BB6A)),
    Purpose("Maps",           Icons.Outlined.Place,         Color(0xFFFF8A65)),
    Purpose("Music",          Icons.Outlined.MusicNote,     Color(0xFFBA68C8)),
    Purpose("Work",           Icons.Outlined.Work,          Color(0xFF4FC3F7)),
    Purpose("Read",           Icons.Outlined.MenuBook,      Color(0xFFCE93D8)),
    Purpose("Camera",         Icons.Outlined.CameraAlt,     Color(0xFFFFD54F)),
    Purpose("Alarm",          Icons.Outlined.Alarm,         Color(0xFFE57373)),
    Purpose("Calendar",       Icons.Outlined.CalendarMonth, Color(0xFF4DB6AC)),
    Purpose("Finance",        Icons.Outlined.TrendingUp,    Color(0xFFA1887F)),
    Purpose(PURPOSE_SEARCH_APP, Icons.Outlined.Search,      Color(0xFF90A4AE)),
)

private val PURPOSE_LABEL_MAP = mapOf(
    "Messages" to "Check messages",
    "Call" to "Make a call",
    "Scan & Pay" to "Scan & Pay",
    "Maps" to "Navigation / Maps",
    "Music" to "Music / Podcast",
    "Work" to "Work / Email",
    "Read" to "Read / Books",
    "Camera" to "Camera",
    "Alarm" to "Alarm / Timer",
    "Calendar" to "Calendar / Tasks",
    "Finance" to "Stocks / Finance",
    PURPOSE_SEARCH_APP to PURPOSE_SEARCH_APP
)

private const val DISMISS_COOLDOWN_SECONDS = 5

// ── Screen ──────────────────────────────────────────────────────────────────

@Composable
fun UnlockPromptScreen(
    disabledPurposes: Set<String> = emptySet(),
    onPurposeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val visiblePurposes = remember(disabledPurposes) {
        PURPOSES.filter { it.label !in disabledPurposes }
    }
    // Dismiss cooldown
    var secondsRemaining by remember { mutableIntStateOf(DISMISS_COOLDOWN_SECONDS) }
    val dismissEnabled = secondsRemaining <= 0

    LaunchedEffect(Unit) {
        repeat(DISMISS_COOLDOWN_SECONDS) {
            delay(1_000)
            secondsRemaining--
        }
    }

    // Breathing animation for glow orb
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGradient)
    ) {
        // Background glow orb
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 80.dp)
                .size(200.dp)
                .scale(glowScale)
                .alpha(glowAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentBlue.copy(alpha = 0.4f),
                            AccentPurple.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            // Header — consistent 28sp with other overlay screens
            Text(
                "What's your\nintention?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    letterSpacing = (-0.5).sp
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = TextPrimary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Choose a purpose before continuing",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Purpose grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(visiblePurposes) { purpose ->
                    PurposeCard(
                        purpose = purpose,
                        onClick = {
                            val recordLabel = PURPOSE_LABEL_MAP[purpose.label] ?: purpose.label
                            onPurposeSelected(recordLabel)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Dismiss button — glass pill consistent with other screens
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (dismissEnabled) Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                            )
                        else Modifier
                    )
                    .background(if (dismissEnabled) CardBg else Color.Transparent)
                    .then(
                        if (dismissEnabled) Modifier.border(
                            1.dp,
                            Color(0x10FFFFFF),
                            RoundedCornerShape(20.dp)
                        )
                        else Modifier
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (dismissEnabled) TextSecondary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (dismissEnabled) "Put the phone down"
                        else "Put the phone down (${secondsRemaining}s)",
                        color = if (dismissEnabled) TextSecondary else TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Purpose Card ────────────────────────────────────────────────────────────

@Composable
private fun PurposeCard(
    purpose: Purpose,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(shape)
            .background(CardBg)
            .border(width = 1.dp, brush = CardBorder, shape = shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container — circular like other screens
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(purpose.tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    purpose.icon,
                    contentDescription = purpose.label,
                    tint = purpose.tint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                purpose.label,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
