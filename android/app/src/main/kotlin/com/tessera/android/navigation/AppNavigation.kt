package com.tessera.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tessera.android.data.KeycloakClient
import com.tessera.android.screens.LoginScreen
import com.tessera.android.screens.MainMenuScreen
import com.tessera.android.screens.ValidateScreen
import com.tessera.android.screens.WelcomeScreen
import com.tessera.android.shared.AuthSession

object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val MAIN_MENU = "main_menu"
    const val VALIDATE = "validate"
}

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
                    val next = if (AuthSession.isAuthenticated) Routes.MAIN_MENU else Routes.LOGIN
                    navController.navigate(next) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onAuthenticated = {
                    navController.navigate(Routes.MAIN_MENU) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN_MENU) {
            val context = LocalContext.current
            MainMenuScreen(
                onValidateClick = { navController.navigate(Routes.VALIDATE) },
                onLogout = {
                    KeycloakClient(context).logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN_MENU) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.VALIDATE) {
            ValidateScreen()
        }
    }
}
