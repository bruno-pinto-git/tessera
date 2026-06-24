package com.tessera.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tessera.android.data.KeycloakClient
import com.tessera.android.screens.LoginScreen
import com.tessera.android.screens.SettingsScreen
import com.tessera.android.screens.WelcomeScreen
import com.tessera.android.shared.AuthSession

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.WELCOME,
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onContinue = {
                    navController.navigate(Routes.SHELL) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onAuthenticated = {
                    navController.navigate(Routes.SHELL) {
                        popUpTo(Routes.SHELL) { inclusive = true }
                    }
                },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SHELL) {
            val context = LocalContext.current
            AppShell(
                onRequestLogin = { navController.navigate(Routes.LOGIN) },
                onLogout = {
                    KeycloakClient(context).logout()
                    AuthSession.clear()
                    navController.navigate(Routes.SHELL) {
                        popUpTo(Routes.SHELL) { inclusive = true }
                    }
                },
            )
        }
    }
}
