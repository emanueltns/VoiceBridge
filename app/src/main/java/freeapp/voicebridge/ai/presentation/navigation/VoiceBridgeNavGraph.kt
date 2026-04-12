package freeapp.voicebridge.ai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import freeapp.voicebridge.ai.presentation.conversation.ConversationScreen
import freeapp.voicebridge.ai.presentation.conversation.ConversationViewModel
import freeapp.voicebridge.ai.presentation.settings.SettingsScreen
import freeapp.voicebridge.ai.presentation.settings.SettingsViewModel

@Composable
fun VoiceBridgeNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Conversation.route,
    ) {
        composable(Screen.Conversation.route) {
            val viewModel: ConversationViewModel = hiltViewModel()
            ConversationScreen(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
