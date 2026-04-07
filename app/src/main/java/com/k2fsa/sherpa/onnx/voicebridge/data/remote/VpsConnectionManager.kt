package com.k2fsa.sherpa.onnx.voicebridge.data.remote

import android.util.Log
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

private const val TAG = "VpsConnectionManager"
private const val MAX_RETRY_DELAY_MS = 60_000L

@Singleton
class VpsConnectionManager @Inject constructor(
    private val vpsClient: VpsClient,
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentHost: String = ""
    private var currentPort: Int = 0
    private var retryCount = 0
    private var retryJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect(host: String, port: Int) {
        currentHost = host
        currentPort = port
        retryCount = 0
        retryJob?.cancel()
        _connectionState.value = ConnectionState.Connected(host, port)
        Log.i(TAG, "Connected to $host:$port")
    }

    fun disconnect() {
        retryJob?.cancel()
        retryCount = 0
        _connectionState.value = ConnectionState.Disconnected
    }

    fun sendMessage(text: String): Result<String> {
        _connectionState.value = ConnectionState.Connecting
        val result = vpsClient.sendAndReceive(currentHost, currentPort, text)

        if (result.isSuccess) {
            retryCount = 0
            _connectionState.value = ConnectionState.Connected(currentHost, currentPort)
        } else {
            scheduleReconnect()
        }

        return result
    }

    private fun scheduleReconnect() {
        val delayMs = min(1000L * (1L shl min(retryCount, 6)), MAX_RETRY_DELAY_MS)
        retryCount++
        _connectionState.value = ConnectionState.Error(
            message = "Connection lost. Retrying in ${delayMs / 1000}s...",
            retryInMs = delayMs,
        )
        Log.w(TAG, "Connection failed, retry #$retryCount in ${delayMs}ms")

        retryJob?.cancel()
        retryJob = scope.launch {
            delay(delayMs)
            _connectionState.value = ConnectionState.Connected(currentHost, currentPort)
        }
    }
}
