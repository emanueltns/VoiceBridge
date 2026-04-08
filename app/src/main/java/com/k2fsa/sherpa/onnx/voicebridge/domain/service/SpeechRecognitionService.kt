package com.k2fsa.sherpa.onnx.voicebridge.domain.service

import kotlinx.coroutines.flow.StateFlow

interface SpeechRecognitionService {
    val partialResult: StateFlow<String>
    fun initialize()
    fun feedAudio(samples: FloatArray)
    fun isEndpoint(): Boolean
    fun getFinalResult(): String
    fun reset()
    fun release()
}
