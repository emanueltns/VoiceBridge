package com.k2fsa.sherpa.onnx.voicebridge.domain.service

import com.k2fsa.sherpa.onnx.voicebridge.domain.model.AudioSegment

interface SpeechRecognitionService {
    fun initialize()
    fun transcribe(segment: AudioSegment): String
    fun release()
}
