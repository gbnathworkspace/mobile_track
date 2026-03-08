package com.mobiletrack.app.presentation.blocked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobiletrack.app.presentation.theme.MobileTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppBlockedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appName = intent.getStringExtra("app_name") ?: "This app"
        val isLimitReached = intent.getBooleanExtra("is_limit_reached", false)

        setContent {
            MobileTrackTheme {
                AppBlockedScreen(
                    appName = appName,
                    isLimitReached = isLimitReached,
                    onGoBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun AppBlockedScreen(
    appName: String,
    isLimitReached: Boolean,
    onGoBack: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Block,
                contentDescription = "Blocked",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(24.dp))

            Text(
                if (isLimitReached) "Time's Up" else "App Blocked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Text(
                if (isLimitReached)
                    "You've reached your daily limit for $appName.\nCome back tomorrow!"
                else
                    "You've blocked $appName.\nStay focused!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(48.dp))

            Button(onClick = onGoBack) {
                Text("Go Back")
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "You set this limit yourself. You've got this.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}
