package com.mobiletrack.app.presentation.blocked

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class ScrollReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appName = intent.getStringExtra("app_name") ?: "this app"
        val reason = intent.getStringExtra("reason") ?: "burst"
        val lockRemainingMs = intent.getLongExtra("lock_remaining_ms", 0L)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* blocked during read timer */ }
        })

        setContent {
            ScrollReminderScreen(
                appName = appName,
                reason = reason,
                lockRemainingMs = lockRemainingMs,
                onDismiss = { finish() }
            )
        }
    }
}

@Composable
fun ScrollReminderScreen(
    appName: String,
    reason: String,
    lockRemainingMs: Long,
    onDismiss: () -> Unit
) {
    val isTempLocked = reason == "temp_locked"
    val isQuietHours = reason == "quiet_hours"
    val isBurst = reason == "burst"

    // Read timer before dismiss is available
    val readTimerSeconds = if (isTempLocked) 3 else 6
    var secondsLeft by remember { mutableIntStateOf(readTimerSeconds) }
    val canDismiss = secondsLeft <= 0

    LaunchedEffect(Unit) {
        repeat(readTimerSeconds) {
            delay(1_000)
            secondsLeft--
        }
    }

    // Lock countdown (live ticking)
    val lockTotalSeconds = (lockRemainingMs / 1000).toInt()
    var lockSecondsLeft by remember { mutableIntStateOf(lockTotalSeconds) }

    LaunchedEffect(lockTotalSeconds) {
        if (lockTotalSeconds > 0) {
            repeat(lockTotalSeconds) {
                delay(1_000)
                lockSecondsLeft--
            }
        }
    }

    // Breathing animation
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

    val icon: ImageVector
    val accentColor: Color
    val title: String
    val subtitle: String
    val bodyLines: List<String>
    val footerText: String

    when {
        isTempLocked -> {
            icon = Icons.Outlined.Lock
            accentColor = Color(0xFFEF5350)
            title = "$appName is paused"
            subtitle = "Take a break. You'll get it back soon."
            bodyLines = listOf(
                "You were scrolling on autopilot, so we hit pause.",
                "Use this time to stretch, drink water, or just breathe.",
                "The urge to check will pass in about 30 seconds."
            )
            footerText = "This is you protecting your own attention."
        }
        isQuietHours -> {
            icon = Icons.Outlined.NightsStay
            accentColor = Color(0xFF7C4DFF)
            title = "Quiet Hours"
            subtitle = "Social media is locked right now."
            bodyLines = listOf(
                "Morning scrolling floods your brain with noise before you've set your own priorities.",
                "Late-night scrolling steals your sleep and tomorrow's energy.",
                "Put the phone down. Read a book. Stretch. Breathe."
            )
            footerText = "Protect your first and last hour."
        }
        else -> {
            icon = Icons.Outlined.SelfImprovement
            accentColor = Color(0xFF3D5AFE)
            title = "Slow Down"
            subtitle = "20 scrolls in under 5 minutes on $appName."
            bodyLines = listOf(
                "That's not intentional browsing — that's autopilot.",
                "Your brain is chasing dopamine, not enjoying content.",
                "Take 3 deep breaths. Ask yourself: \"Am I looking for something, or just filling time?\""
            )
            footerText = "You chose to be mindful. This is you keeping that promise."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E21),
                        Color(0xFF0D1B3E),
                        Color(0xFF080C1A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated icon ring
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(breathScale),
                contentAlignment = Alignment.Center
            ) {
                // Glowing ring
                if (lockSecondsLeft > 0) {
                    val progress = lockSecondsLeft.toFloat() / lockTotalSeconds.coerceAtLeast(1)
                    Canvas(modifier = Modifier.size(100.dp)) {
                        val strokeWidth = 4.dp.toPx()
                        val arcSize = size.width - strokeWidth
                        drawArc(
                            color = accentColor.copy(alpha = 0.15f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = accentColor
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp
                ),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7B8CB8),
                textAlign = TextAlign.Center
            )

            // Lock countdown
            if (lockSecondsLeft > 0) {
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Locked for ${formatCountdown(lockSecondsLeft)}",
                        color = accentColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Body messages
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x14FFFFFF))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0x20FFFFFF), Color(0x08FFFFFF))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                bodyLines.forEachIndexed { index, line ->
                    Row {
                        Text(
                            text = "${index + 1}",
                            color = accentColor.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = line,
                            color = Color(0xFFCCD6E8),
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Dismiss button
            Button(
                onClick = onDismiss,
                enabled = canDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    disabledContainerColor = Color(0xFF1A2040)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    if (canDismiss) "I understand"
                    else "Take a moment... (${secondsLeft}s)",
                    color = if (canDismiss) Color.White else Color(0xFF4A5070),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                footerText,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4A5A78),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatCountdown(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "${m}:${s.toString().padStart(2, '0')}"
}
