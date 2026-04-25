package com.mobiletrack.app.presentation.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

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
private val AccentGreen = Color(0xFF66BB6A)

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val accentColor: Color,
    val actionLabel: String? = null,
    val onAction: ((android.content.Context) -> Unit)? = null
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Outlined.PhoneAndroid,
            title = "Take Back Control",
            description = "MobileTrack helps you break the cycle of mindless scrolling, peeking, and endless social media rabbit holes.",
            accentColor = AccentBlue
        ),
        OnboardingPage(
            icon = Icons.Outlined.QueryStats,
            title = "Grant Usage Access",
            description = "We need permission to see which apps you use and for how long. This data never leaves your phone.",
            accentColor = AccentPurple,
            actionLabel = "Grant Usage Access",
            onAction = { ctx ->
                ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        ),
        OnboardingPage(
            icon = Icons.Outlined.Accessibility,
            title = "Enable Accessibility",
            description = "This lets MobileTrack block apps that exceed your limit and detect when you're in an infinite scroll session.",
            accentColor = AccentTeal,
            actionLabel = "Enable Accessibility",
            onAction = { ctx ->
                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        ),
        OnboardingPage(
            icon = Icons.Outlined.Layers,
            title = "Allow Overlay",
            description = "The unlock prompt screen needs permission to appear on top of other apps when you pick up your phone.",
            accentColor = Color(0xFFFF8A65),
            actionLabel = "Allow Overlay",
            onAction = { ctx ->
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")
                    )
                )
            }
        ),
        OnboardingPage(
            icon = Icons.Outlined.CheckCircle,
            title = "You're All Set!",
            description = "MobileTrack is ready. Set your first app limit and start building better phone habits today.",
            accentColor = AccentGreen
        )
    )

    val pagerState = rememberPagerState { pages.size }

    // Breathing animation for icon
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animated icon
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
                                .background(page.accentColor.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(page.accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    page.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = page.accentColor
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(36.dp))

                    Text(
                        page.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    if (page.actionLabel != null && page.onAction != null) {
                        Spacer(Modifier.height(28.dp))
                        Button(
                            onClick = { page.onAction.invoke(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = page.accentColor
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                page.actionLabel,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Page indicators
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val page = pages[index]
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isSelected) page.accentColor
                                else Color(0x20FFFFFF)
                            )
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }) {
                        Text("Back", color = TextSecondary, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                val isLast = pagerState.currentPage == pages.size - 1
                val currentPage = pages[pagerState.currentPage]

                Button(
                    onClick = {
                        if (isLast) {
                            viewModel.completeOnboarding()
                            onComplete()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentPage.accentColor
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        if (isLast) "Get Started" else "Next",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!isLast) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
