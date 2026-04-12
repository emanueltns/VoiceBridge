package freeapp.voicebridge.ai.domain.model

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val host: String, val port: Int) : ConnectionState()
    data class Error(val message: String, val retryInMs: Long? = null) : ConnectionState()
}
