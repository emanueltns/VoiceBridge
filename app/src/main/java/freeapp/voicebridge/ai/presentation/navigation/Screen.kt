package freeapp.voicebridge.ai.presentation.navigation

sealed class Screen(val route: String) {
    data object Conversation : Screen("conversation")
    data object Settings : Screen("settings")
}
