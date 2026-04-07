package com.k2fsa.sherpa.onnx.voicebridge.domain.model

data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val text: String,
    val timestamp: Long,
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
}
