package com.k2fsa.sherpa.onnx.voicebridge.domain.service

import com.k2fsa.sherpa.onnx.voicebridge.domain.model.AudioSegment

interface VoiceActivityDetector {
    fun initialize()
    fun feedAudio(samples: FloatArray)
    fun hasSpeechSegment(): Boolean
    fun getSpeechSegment(): AudioSegment
    fun reset()
    fun release()
}
