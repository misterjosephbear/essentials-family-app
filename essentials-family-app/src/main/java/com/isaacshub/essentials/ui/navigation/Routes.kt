package com.isaacshub.essentials.ui.navigation

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Setup : Routes("setup")
    object Home : Routes("home")
    object ChoreDetail : Routes("chore/{choreId}") {
        fun createRoute(choreId: Long) = "chore/$choreId"
    }
    object PhotoCapture : Routes("photo/{choreId}") {
        fun createRoute(choreId: Long) = "photo/$choreId"
    }
}
