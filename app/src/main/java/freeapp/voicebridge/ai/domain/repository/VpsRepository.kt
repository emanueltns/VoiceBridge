package freeapp.voicebridge.ai.domain.repository

import freeapp.voicebridge.ai.domain.model.ConnectionState
import kotlinx.coroutines.flow.StateFlow

interface VpsRepository {
    val connectionState: StateFlow<ConnectionState>
    suspend fun connect(host: String, port: Int)
    suspend fun disconnect()
    suspend fun sendMessage(text: String): Result<String>
    suspend fun sendMessageStreaming(text: String, onChunk: (String) -> Unit): Result<String>
}
