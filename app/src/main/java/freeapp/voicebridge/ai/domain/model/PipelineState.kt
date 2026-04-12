package freeapp.voicebridge.ai.domain.model

enum class PipelineState {
    IDLE,
    INITIALIZING,
    LISTENING,
    TRANSCRIBING,
    SENDING,
    SPEAKING,
    ENTERTAINING,
}
