package freeapp.voicebridge.ai.domain.service

import kotlinx.coroutines.flow.StateFlow

interface TextToSpeechService {
    val isSpeaking: StateFlow<Boolean>
    fun initialize()
    fun setSpeakerId(sid: Int)
    fun numSpeakers(): Int
    suspend fun speak(text: String)
    fun stop()
    fun release()
}
