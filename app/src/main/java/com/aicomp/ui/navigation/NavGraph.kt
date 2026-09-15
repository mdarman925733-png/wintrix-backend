package com.aicomp.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aicomp.data.repository.AuthRepository
import com.aicomp.ui.screens.auth.LoginScreen
import com.aicomp.ui.screens.auth.ProfileSetupScreen
import com.aicomp.ui.screens.call.CallScreen
import com.aicomp.ui.screens.chat.ChatScreen
import com.aicomp.ui.screens.home.HomeScreen
import com.aicomp.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(navController: NavHostController) {

    // Determine start destination based on auth + profile status
    var startDestination by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            startDestination = when {
                AuthRepository.currentUser == null -> Screen.Login.route
                !AuthRepository.isProfileComplete() -> Screen.ProfileSetup.route
                else -> Screen.Home.route
            }
        }
    }

    if (startDestination == null) return // Splash/loading — nothing yet

    NavHost(
        navController = navController,
        startDestination = startDestination!!,
        enterTransition = { slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut() }
    ) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.ProfileSetup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ProfileSetup.route) {
            ProfileSetupScreen(
                onProfileSaved = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onCompanionClick = { id ->
                    navController.navigate(Screen.Chat.createRoute(id))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("companionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: ""
            ChatScreen(
                companionId = companionId,
                onCallClick = { navController.navigate(Screen.Call.createRoute(companionId)) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Call.route,
            arguments = listOf(navArgument("companionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val companionId = backStackEntry.arguments?.getString("companionId") ?: ""
            CallScreen(
                companionId = companionId,
                onEndCall = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
