package com.k2fsa.sherpa.onnx.voicebridge.domain.repository

import com.k2fsa.sherpa.onnx.voicebridge.domain.model.ConnectionState
import kotlinx.coroutines.flow.StateFlow

interface VpsRepository {
    val connectionState: StateFlow<ConnectionState>
    suspend fun connect(host: String, port: Int)
    suspend fun disconnect()
    suspend fun sendMessage(text: String): Result<String>
}
