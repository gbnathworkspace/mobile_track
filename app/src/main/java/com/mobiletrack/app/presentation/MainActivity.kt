package com.mobiletrack.app.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mobiletrack.app.presentation.navigation.NavGraph
import com.mobiletrack.app.presentation.navigation.Screen
import com.mobiletrack.app.presentation.theme.MobileTrackTheme
import com.mobiletrack.app.service.TrackingService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start tracking service
        startForegroundService(Intent(this, TrackingService::class.java))

        setContent {
            MobileTrackTheme(darkTheme = true) {
                val onboardingComplete by viewModel.onboardingComplete.collectAsStateWithLifecycle(null)
                val navController = rememberNavController()

                // Wait until the preference is loaded before rendering NavHost
                if (onboardingComplete == null) return@MobileTrackTheme

                val startDestination = if (onboardingComplete == true) Screen.Dashboard.route else Screen.Onboarding.route

                val bottomNavItems = listOf(
                    BottomNavItem(Screen.Dashboard, Icons.Default.Home, "Dashboard"),
                    BottomNavItem(Screen.AppLimits, Icons.Default.Timer, "Limits"),
                    BottomNavItem(Screen.Rules, Icons.Default.Shield, "Rules"),
                    BottomNavItem(Screen.Reports, Icons.Default.BarChart, "Reports"),
                )

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val showBottomBar = bottomNavItems.any { it.screen.route == currentDestination?.route }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                bottomNavItems.forEach { item ->
                                    NavigationBarItem(
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) },
                                        selected = currentDestination?.hierarchy?.any {
                                            it.route == item.screen.route
                                        } == true,
                                        onClick = {
                                            navController.navigate(item.screen.route) {
                                                popUpTo(Screen.Dashboard.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(padding)
                    ) {
                        NavGraph(
                            navController = navController,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}

data class BottomNavItem(
    val screen: Screen,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
