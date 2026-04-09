package com.k2fsa.sherpa.onnx.voicebridge.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.ConversationScreen
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.ConversationViewModel
import com.k2fsa.sherpa.onnx.voicebridge.presentation.settings.SettingsScreen
import com.k2fsa.sherpa.onnx.voicebridge.presentation.settings.SettingsViewModel

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
