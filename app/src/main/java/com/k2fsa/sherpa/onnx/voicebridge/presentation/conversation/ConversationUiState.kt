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
    val audioAmplitude: Float = 0f,
    val isMuted: Boolean = false,
    val modelsReady: Boolean = false,
    val streamingResponse: String = "",
    val userName: String = "",
    val needsSetup: Boolean = false,
)

sealed class ConversationIntent {
    data object Start : ConversationIntent()
    data object Stop : ConversationIntent()
    data object DismissError : ConversationIntent()
    data object ToggleMute : ConversationIntent()
    data class SendText(val text: String) : ConversationIntent()
}
