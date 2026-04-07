package com.k2fsa.sherpa.onnx.voicebridge.domain.model

enum class PipelineState {
    IDLE,
    INITIALIZING,
    LISTENING,
    TRANSCRIBING,
    SENDING,
    SPEAKING,
    ENTERTAINING,
}
