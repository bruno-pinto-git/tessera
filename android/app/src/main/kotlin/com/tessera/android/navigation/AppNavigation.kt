package com.tessera.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tessera.android.screens.MainMenuScreen
import com.tessera.android.screens.ValidateScreen
import com.tessera.android.screens.WelcomeScreen

object Routes {
    const val WELCOME = "welcome"
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
                    navController.navigate(Routes.MAIN_MENU) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN_MENU) {
            MainMenuScreen(
                onValidateClick = { navController.navigate(Routes.VALIDATE) },
            )
        }
        composable(Routes.VALIDATE) {
            ValidateScreen()
        }
    }
}
