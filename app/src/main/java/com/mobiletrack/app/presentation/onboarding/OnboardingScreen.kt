package com.mobiletrack.app.presentation.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobiletrack.app.presentation.design.MTIconSize
import com.mobiletrack.app.presentation.design.MTSpacing
import com.mobiletrack.app.presentation.design.components.MTPrimaryButton
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
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
            icon = Icons.Default.PhoneAndroid,
            title = "Take Back Control",
            description = "MobileTrack helps you break the cycle of mindless scrolling, peeking, and endless social media rabbit holes."
        ),
        OnboardingPage(
            icon = Icons.Default.QueryStats,
            title = "Grant Usage Access",
            description = "We need permission to see which apps you use and for how long. This data never leaves your phone.",
            actionLabel = "Grant Usage Access",
            onAction = { ctx ->
                ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        ),
        OnboardingPage(
            icon = Icons.Default.Accessibility,
            title = "Enable Accessibility",
            description = "This lets MobileTrack block apps that exceed your limit and detect when you're in an infinite scroll session.",
            actionLabel = "Enable Accessibility",
            onAction = { ctx ->
                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        ),
        OnboardingPage(
            icon = Icons.Default.Layers,
            title = "Allow Overlay",
            description = "The unlock prompt screen needs permission to appear on top of other apps when you pick up your phone.",
            actionLabel = "Allow Overlay",
            onAction = { ctx ->
                ctx.startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${ctx.packageName}")
                ))
            }
        ),
        OnboardingPage(
            icon = Icons.Default.CheckCircle,
            title = "You're All Set!",
            description = "MobileTrack is ready. Set your first app limit and start building better phone habits today."
        )
    )

    val pagerState = rememberPagerState { pages.size }

    Column(
        modifier = Modifier.fillMaxSize(),
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
                    .padding(MTSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(MTIconSize.hero),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(MTSpacing.xl))
                Text(
                    page.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(MTSpacing.md))
                Text(
                    page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline
                )
                if (page.actionLabel != null && page.onAction != null) {
                    Spacer(Modifier.height(MTSpacing.xl))
                    MTPrimaryButton(
                        text = page.actionLabel,
                        onClick = { page.onAction.invoke(context) }
                    )
                }
            }
        }

        // Page indicator
        Row(
            modifier = Modifier.padding(MTSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(MTSpacing.sm)
        ) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                Surface(
                    modifier = Modifier.size(if (selected) 12.dp else 8.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ) {}
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MTSpacing.lg, vertical = MTSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) { Text("Back") }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            val isLast = pagerState.currentPage == pages.size - 1
            MTPrimaryButton(
                text = if (isLast) "Get Started" else "Next",
                onClick = {
                if (isLast) {
                    viewModel.completeOnboarding()
                    onComplete()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
                }
            )
        }

        Spacer(Modifier.height(MTSpacing.md))
    }
}
