package com.mobiletrack.app.presentation.unlock

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

// ── Colors ──────────────────────────────────────────────────────────────────
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFE0C8FF)
private val TextMuted = Color(0xFF9878B0)
private val AccentSunrise = Color(0xFFE8956A)
private val AccentGold = Color(0xFFFFD93D)
private val CardBg = Color(0x20FFFFFF)
private val CardBorder = Brush.linearGradient(listOf(Color(0x30FFFFFF), Color(0x10FFFFFF)))

@AndroidEntryPoint
class WakeUpPromptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            WakeUpScreen(onDismiss = { finish() })
        }
    }
}

@Composable
fun WakeUpScreen(onDismiss: () -> Unit) {
    // 8-second read timer
    var secondsLeft by remember { mutableIntStateOf(8) }
    val canDismiss = secondsLeft <= 0

    LaunchedEffect(Unit) {
        repeat(8) {
            delay(1_000)
            secondsLeft--
        }
    }

    // Breathing animation — consistent with other overlay screens
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    val morningGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A0A2E),
            Color(0xFF2D1B69),
            Color(0xFF562B7C)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(morningGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated icon ring — same pattern as ScrollReminder/AppBlocked
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(breathScale),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(AccentGold.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AccentGold.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.WbSunny,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = AccentGold
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Title — consistent 28sp with all overlay screens
            Text(
                "Good Morning",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp
                ),
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "You just woke up.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // Body messages — numbered glass card, same as ScrollReminder/AppBlocked
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .border(width = 1.dp, brush = CardBorder, shape = RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val bodyLines = listOf(
                    "The first thing you look at shapes your entire day. Your mind is fresh — don't flood it.",
                    "Scrolling through feeds fills your brain with other people's priorities before you've set your own.",
                    "Drink water. Stretch for 2 minutes. Think about one thing you want to accomplish today.",
                    "Your phone will still be here in 30 minutes. Your morning clarity won't."
                )

                bodyLines.forEachIndexed { index, line ->
                    Row {
                        Text(
                            text = "${index + 1}",
                            color = AccentGold.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = line,
                            color = Color(0xFFE0D0F0),
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Dismiss button — consistent with all overlay screens
            Button(
                onClick = onDismiss,
                enabled = canDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentSunrise,
                    disabledContainerColor = Color(0xFF3A2050)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    if (canDismiss) "I'll be intentional today"
                    else "Take a moment... (${secondsLeft}s)",
                    color = if (canDismiss) Color.White else Color(0xFF7858A0),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "Your morning routine matters more than any notification.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
