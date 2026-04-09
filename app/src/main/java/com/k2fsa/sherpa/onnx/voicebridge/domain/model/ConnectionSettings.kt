package com.k2fsa.sherpa.onnx.voicebridge.domain.model

data class ConnectionSettings(
    val host: String = "",
    val port: Int = 9999,
    val voiceId: Int = 0,
)
