package com.tessera.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tessera.android.screens.MainMenuScreen
import com.tessera.android.screens.ValidateScreen

object Routes {
    const val MAIN_MENU = "main_menu"
    const val VALIDATE = "validate"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN_MENU
    ) {
        composable(Routes.MAIN_MENU) {
            MainMenuScreen(
                onValidateClick = { navController.navigate(Routes.VALIDATE) }
            )
        }
        composable(Routes.VALIDATE) {
            ValidateScreen()
        }
    }
}
