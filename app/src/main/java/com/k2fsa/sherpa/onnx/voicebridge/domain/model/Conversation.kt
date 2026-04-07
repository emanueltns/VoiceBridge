package com.k2fsa.sherpa.onnx.voicebridge.domain.model

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
)
