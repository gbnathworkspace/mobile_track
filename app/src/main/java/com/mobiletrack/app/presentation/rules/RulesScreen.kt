package com.mobiletrack.app.presentation.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mobiletrack.app.presentation.design.MTIconSize
import com.mobiletrack.app.presentation.design.MTSpacing
import com.mobiletrack.app.presentation.design.components.MTCard
import com.mobiletrack.app.presentation.design.components.MTIconRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    navController: NavController,
    viewModel: RulesViewModel = hiltViewModel()
) {
    val maxUnlocks by viewModel.maxUnlocks.collectAsStateWithLifecycle(50)
    val promptEnabled by viewModel.promptEnabled.collectAsStateWithLifecycle(true)
    var unlockSlider by remember { mutableFloatStateOf((maxUnlocks ?: 50).toFloat()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rules & Focus") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(MTSpacing.md),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Unlock prompt toggle
            RuleCard(
                icon = Icons.Default.Psychology,
                title = "Unlock Purpose Prompt",
                subtitle = "Ask 'Why are you here?' every time you unlock your phone"
            ) {
                Switch(
                    checked = promptEnabled ?: true,
                    onCheckedChange = { viewModel.setPromptEnabled(it) }
                )
            }

            // Max unlocks per day
            MTCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(MTSpacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Max Unlocks Per Day", fontWeight = FontWeight.SemiBold)
                            Text(
                                "${unlockSlider.toInt()} unlocks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Slider(
                        value = unlockSlider,
                        onValueChange = { unlockSlider = it },
                        onValueChangeFinished = { viewModel.setMaxUnlocks(unlockSlider.toInt()) },
                        valueRange = 10f..200f,
                        steps = 18,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("10", style = MaterialTheme.typography.labelSmall)
                        Text("200", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Focus hours placeholder
            RuleCard(
                icon = Icons.Default.DoNotDisturbOn,
                title = "Focus Hours",
                subtitle = "Block distracting apps during work/study hours"
            ) {
                FilledTonalButton(onClick = { /* navigate to focus hours setup */ }) {
                    Text("Set Up")
                }
            }
        }
    }
}

@Composable
fun RuleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    action: @Composable () -> Unit
) {
    MTCard(modifier = Modifier.fillMaxWidth()) {
        MTIconRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            iconTint = MaterialTheme.colorScheme.primary,
            trailing = {
            action()
            }
        )
    }
}
