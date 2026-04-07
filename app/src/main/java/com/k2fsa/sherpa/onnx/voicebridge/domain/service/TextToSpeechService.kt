package com.k2fsa.sherpa.onnx.voicebridge.domain.service

import kotlinx.coroutines.flow.StateFlow

interface TextToSpeechService {
    val isSpeaking: StateFlow<Boolean>
    fun initialize()
    suspend fun speak(text: String)
    fun stop()
    fun release()
}
