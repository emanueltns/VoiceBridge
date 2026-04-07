package com.k2fsa.sherpa.onnx.voicebridge.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.ConversationScreen
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.ConversationViewModel
import com.k2fsa.sherpa.onnx.voicebridge.presentation.history.HistoryScreen
import com.k2fsa.sherpa.onnx.voicebridge.presentation.history.HistoryViewModel
import com.k2fsa.sherpa.onnx.voicebridge.presentation.settings.SettingsBottomSheet
import com.k2fsa.sherpa.onnx.voicebridge.presentation.settings.SettingsViewModel

@Composable
fun VoiceBridgeNavGraph() {
    val navController = rememberNavController()
    var showSettings by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = Screen.Conversation.route,
    ) {
        composable(Screen.Conversation.route) {
            val viewModel: ConversationViewModel = hiltViewModel()
            ConversationScreen(
                viewModel = viewModel,
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onOpenSettings = { showSettings = true },
            )

            if (showSettings) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsBottomSheet(
                    viewModel = settingsViewModel,
                    onDismiss = { showSettings = false },
                )
            }
        }

        composable(Screen.History.route) {
            val viewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
