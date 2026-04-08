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
    val callStartTimeMs: Long? = null,
)

sealed class ConversationIntent {
    data object Start : ConversationIntent()
    data object Stop : ConversationIntent()
    data object NewConversation : ConversationIntent()
    data object DismissError : ConversationIntent()
    data object OpenHistory : ConversationIntent()
    data object OpenSettings : ConversationIntent()
}
