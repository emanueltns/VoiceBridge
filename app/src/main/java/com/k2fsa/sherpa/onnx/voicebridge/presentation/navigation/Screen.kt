package com.k2fsa.sherpa.onnx.voicebridge.presentation.navigation

sealed class Screen(val route: String) {
    data object Conversation : Screen("conversation")
    data object History : Screen("history")
    data object ConversationDetail : Screen("conversation_detail/{conversationId}") {
        fun createRoute(conversationId: String) = "conversation_detail/$conversationId"
    }
}
