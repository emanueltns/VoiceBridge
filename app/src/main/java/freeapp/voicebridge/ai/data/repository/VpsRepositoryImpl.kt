package freeapp.voicebridge.ai.data.repository

import freeapp.voicebridge.ai.data.remote.VpsConnectionManager
import freeapp.voicebridge.ai.domain.model.ConnectionState
import freeapp.voicebridge.ai.domain.repository.VpsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpsRepositoryImpl @Inject constructor(
    private val connectionManager: VpsConnectionManager,
) : VpsRepository {

    override val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

    override suspend fun connect(host: String, port: Int) {
        connectionManager.connect(host, port)
    }

    override suspend fun disconnect() {
        connectionManager.disconnect()
    }

    override suspend fun sendMessage(text: String): Result<String> {
        return withContext(Dispatchers.IO) {
            connectionManager.sendMessage(text)
        }
    }

    override suspend fun sendMessageStreaming(text: String, onChunk: (String) -> Unit): Result<String> {
        return withContext(Dispatchers.IO) {
            connectionManager.sendMessageStreaming(text, onChunk)
        }
    }
}
