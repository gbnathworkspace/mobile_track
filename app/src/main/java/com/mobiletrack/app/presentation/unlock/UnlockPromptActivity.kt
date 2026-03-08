package com.mobiletrack.app.presentation.unlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobiletrack.app.presentation.theme.MobileTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UnlockPromptActivity : ComponentActivity() {

    private val viewModel: UnlockPromptViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileTrackTheme {
                UnlockPromptScreen(
                    onPurposeSelected = { purpose ->
                        viewModel.recordPurpose(purpose)
                        finish()
                    },
                    onDismiss = {
                        viewModel.recordNoPurpose()
                        finish()
                    }
                )
            }
        }
    }
}

val UNLOCK_PURPOSES = listOf(
    "Check messages",
    "Make a call",
    "Navigation / Maps",
    "Music / Podcast",
    "Work / Email",
    "Camera",
    "Alarm / Timer",
    "Calendar / Tasks",
    "News / Info",
    "Just browsing..."
)

@Composable
fun UnlockPromptScreen(
    onPurposeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                "Why are you picking up your phone?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
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
                    OutlinedButton(
                        onClick = { onPurposeSelected(purpose) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(purpose, textAlign = TextAlign.Center)
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
