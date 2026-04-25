package com.mobiletrack.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mobiletrack.app.presentation.applimits.AppLimitsScreen
import com.mobiletrack.app.presentation.cleanup.AppCleanupScreen
import com.mobiletrack.app.presentation.dashboard.DashboardScreen
import com.mobiletrack.app.presentation.onboarding.OnboardingScreen
import com.mobiletrack.app.presentation.reports.ReportsScreen
import com.mobiletrack.app.presentation.rules.RulesScreen
import com.mobiletrack.app.presentation.settings.PurposeEditorScreen
import com.mobiletrack.app.presentation.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object AppLimits : Screen("app_limits")
    object Rules : Screen("rules")
    object Reports : Screen("reports")
    object Settings : Screen("settings")
    object PurposeEditor : Screen("purpose_editor")
    object AppCleanup : Screen("app_cleanup")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onComplete = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }
        composable(Screen.AppLimits.route) {
            AppLimitsScreen(navController = navController)
        }
        composable(Screen.Rules.route) {
            RulesScreen(navController = navController)
        }
        composable(Screen.Reports.route) {
            ReportsScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.PurposeEditor.route) {
            PurposeEditorScreen(navController = navController)
        }
        composable(Screen.AppCleanup.route) {
            AppCleanupScreen(navController = navController)
        }
    }
}
