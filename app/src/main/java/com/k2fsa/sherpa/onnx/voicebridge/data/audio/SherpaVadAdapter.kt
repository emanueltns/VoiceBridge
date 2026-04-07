package com.k2fsa.sherpa.onnx.voicebridge.data.audio

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.AudioSegment
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.VoiceActivityDetector
import javax.inject.Inject

class SherpaVadAdapter @Inject constructor(
    private val assetManager: AssetManager,
) : VoiceActivityDetector {

    private var vad: Vad? = null

    override fun initialize() {
        val config = VadModelConfig(
            sileroVadModelConfig = SileroVadModelConfig(
                model = "silero_vad.onnx",
                threshold = 0.5f,
                minSilenceDuration = 0.8f,   // wait 800ms of silence before cutting
                minSpeechDuration = 0.5f,    // ignore speech shorter than 500ms
                windowSize = 512,
                maxSpeechDuration = 30.0f,   // allow up to 30s of continuous speech
            ),
            sampleRate = 16000,
            numThreads = 1,
            provider = "cpu",
        )
        vad = Vad(assetManager = assetManager, config = config)
    }

    override fun feedAudio(samples: FloatArray) {
        vad?.acceptWaveform(samples)
    }

    override fun hasSpeechSegment(): Boolean {
        return vad?.empty() == false
    }

    override fun getSpeechSegment(): AudioSegment {
        val v = vad!!
        val segment = v.front()
        v.pop()
        return AudioSegment(segment.samples)
    }

    override fun reset() {
        vad?.reset()
    }

    override fun release() {
        vad?.release()
        vad = null
    }
}
