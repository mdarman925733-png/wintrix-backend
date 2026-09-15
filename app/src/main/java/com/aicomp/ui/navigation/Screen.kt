package com.aicomp.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object ProfileSetup : Screen("profile_setup")
    object Home : Screen("home")

    object Chat : Screen("chat/{companionId}") {
        fun createRoute(companionId: String) = "chat/$companionId"
    }

    object Call : Screen("call/{companionId}") {
        fun createRoute(companionId: String) = "call/$companionId"
    }

    object Settings : Screen("settings")
}
