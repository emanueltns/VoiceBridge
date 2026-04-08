package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation

import com.k2fsa.sherpa.onnx.voicebridge.domain.model.ConnectionState
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Message
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState

data class ConversationUiState(
    val pipelineState: PipelineState = PipelineState.IDLE,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val messages: List<Message> = emptyList(),
    val isRunning: Boolean = false,
    val error: String? = null,
    val partialTranscript: String = "",
)

sealed class ConversationIntent {
    data object Start : ConversationIntent()
    data object Stop : ConversationIntent()
    data object DismissError : ConversationIntent()
    data object OpenHistory : ConversationIntent()
    data object OpenSettings : ConversationIntent()
}
