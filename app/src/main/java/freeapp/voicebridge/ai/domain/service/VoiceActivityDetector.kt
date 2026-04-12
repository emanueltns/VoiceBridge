package freeapp.voicebridge.ai.domain.service

import freeapp.voicebridge.ai.domain.model.AudioSegment

interface VoiceActivityDetector {
    fun initialize()
    fun feedAudio(samples: FloatArray)
    fun hasSpeechSegment(): Boolean
    fun getSpeechSegment(): AudioSegment
    fun reset()
    fun release()
}
