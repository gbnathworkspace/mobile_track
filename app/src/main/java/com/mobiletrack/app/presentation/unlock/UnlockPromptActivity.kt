package com.mobiletrack.app.presentation.unlock

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mobiletrack.app.presentation.theme.GlassColors
import com.mobiletrack.app.presentation.theme.UnlockTheme
import com.mobiletrack.app.presentation.theme.UnlockPromptTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UnlockPromptActivity : ComponentActivity() {

    private val viewModel: UnlockPromptViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("UnlockPromptActivity", "onCreate")

        // Full screen — draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContent {
            val theme by viewModel.unlockTheme.collectAsStateWithLifecycle(UnlockTheme.LIGHT)

            UnlockPromptTheme(theme = theme) {
                UnlockPromptScreen(
                    theme = theme,
                    onPurposeSelected = { purpose ->
                        lifecycleScope.launch {
                            Log.d("UnlockPromptActivity", "Purpose selected: $purpose")
                            viewModel.recordPurpose(purpose)
                            val intent = Intent(this@UnlockPromptActivity, PurposeAppLauncherActivity::class.java).apply {
                                putExtra(EXTRA_PURPOSE, purpose)
                                addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                )
                            }
                            startActivity(intent)
                            finish()
                        }
                    },
                    onDismiss = {
                        lifecycleScope.launch {
                            Log.d("UnlockPromptActivity", "Dismissed without purpose")
                            viewModel.recordNoPurpose()
                            finish()
                        }
                    }
                )
            }
        }
    }
}

const val PURPOSE_SEARCH_APP = "Search App"

val UNLOCK_PURPOSES = listOf(
    "Check messages",
    "Make a call",
    "Navigation / Maps",
    "Music / Podcast",
    "Work / Email",
    "Camera",
    "Alarm / Timer",
    "Calendar / Tasks",
    "Stocks / Finance",
    PURPOSE_SEARCH_APP
)

@Composable
fun UnlockPromptScreen(
    theme: UnlockTheme,
    onPurposeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isGlass = theme == UnlockTheme.GLASS

    val backgroundModifier = if (isGlass) {
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1B3E), Color(0xFF060D1F))
                )
            )
    } else {
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    }

    Box(modifier = backgroundModifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                "Why are you picking up your phone?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Choose a purpose to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(UNLOCK_PURPOSES) { purpose ->
                    if (isGlass) {
                        GlassPurposeButton(purpose = purpose, onClick = { onPurposeSelected(purpose) })
                    } else {
                        OutlinedButton(
                            onClick = { onPurposeSelected(purpose) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(purpose, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.outline
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Put the phone down")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GlassPurposeButton(purpose: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GlassColors.surfaceVariant)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x80B8D4FF), Color(0x30FFFFFF))
                ),
                shape = shape
            ),
        shape = shape
    ) {
        Text(
            purpose,
            textAlign = TextAlign.Center,
            color = GlassColors.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
