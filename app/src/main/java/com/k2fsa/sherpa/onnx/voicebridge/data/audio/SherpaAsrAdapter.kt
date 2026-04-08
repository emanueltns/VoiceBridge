package com.k2fsa.sherpa.onnx.voicebridge.data.audio

import android.content.res.AssetManager
import android.util.Log
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.EndpointRule
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.SpeechRecognitionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

private const val TAG = "StreamingASR"

private val HALLUCINATION_PATTERNS = listOf(
    "thank you", "thanks for watching", "subscribe",
    "like and subscribe", "please subscribe", "see you next time",
    "bye bye", "goodbye", "the end",
)

class SherpaAsrAdapter @Inject constructor(
    private val assetManager: AssetManager,
) : SpeechRecognitionService {

    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private val sampleRate = 16000

    private val _partialResult = MutableStateFlow("")
    override val partialResult: StateFlow<String> = _partialResult.asStateFlow()

    override fun initialize() {
        val modelDir = "sherpa-onnx-streaming-zipformer-en"
        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = sampleRate, featureDim = 80),
            modelConfig = OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = "$modelDir/encoder.int8.onnx",
                    decoder = "$modelDir/decoder.int8.onnx",
                    joiner = "$modelDir/joiner.int8.onnx",
                ),
                tokens = "$modelDir/tokens.txt",
                numThreads = 2,
                debug = false,
                provider = "cpu",
                modelType = "zipformer2",
            ),
            endpointConfig = EndpointConfig(
                rule1 = EndpointRule(false, 1.8f, 0.0f),  // 1.8s silence = endpoint
                rule2 = EndpointRule(true, 1.0f, 0.0f),   // 1.0s silence after speech
                rule3 = EndpointRule(false, 0.0f, 30.0f),  // 30s max utterance
            ),
            enableEndpoint = true,
            decodingMethod = "greedy_search",
        )
        recognizer = OnlineRecognizer(assetManager = assetManager, config = config)
        stream = recognizer!!.createStream()
        Log.i(TAG, "Streaming ASR initialized (Zipformer)")
    }

    override fun feedAudio(samples: FloatArray) {
        val s = stream ?: return
        s.acceptWaveform(samples, sampleRate)

        val rec = recognizer ?: return
        while (rec.isReady(s)) {
            rec.decode(s)
        }

        // Update partial result
        val result = rec.getResult(s)
        if (result.text.isNotBlank()) {
            _partialResult.value = result.text.trim()
        }
    }

    override fun isEndpoint(): Boolean {
        val rec = recognizer ?: return false
        val s = stream ?: return false
        return rec.isEndpoint(s)
    }

    override fun getFinalResult(): String {
        val rec = recognizer ?: return ""
        val s = stream ?: return ""

        val result = rec.getResult(s)
        val text = result.text.trim()

        // Reset stream for next utterance
        rec.reset(s)
        _partialResult.value = ""

        // Filter hallucinations
        val lower = text.lowercase()
        if (lower.isEmpty()) return ""
        if (HALLUCINATION_PATTERNS.any { lower == it || lower == "$it." }) {
            Log.d(TAG, "Filtered hallucination: $text")
            return ""
        }

        return text
    }

    override fun reset() {
        val rec = recognizer ?: return
        val s = stream ?: return
        rec.reset(s)
        _partialResult.value = ""
    }

    override fun release() {
        stream?.release()
        stream = null
        recognizer?.release()
        recognizer = null
    }
}
