package freeapp.voicebridge.ai.data.audio

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
import freeapp.voicebridge.ai.domain.service.SpeechRecognitionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

private const val TAG = "StreamingASR"

class SherpaAsrAdapter @Inject constructor(
    private val assetManager: AssetManager,
) : SpeechRecognitionService {

    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private val sampleRate = 16000

    private val _partialResult = MutableStateFlow("")
    override val partialResult: StateFlow<String> = _partialResult.asStateFlow()

    override fun initialize() {
        val modelDir = "sherpa-onnx-nemotron-en"

        // Check if model files exist in assets before initializing
        try {
            assetManager.list(modelDir)?.let { files ->
                if (!files.contains("encoder.int8.onnx")) {
                    Log.w(TAG, "Nemotron model not found in assets — offline ASR unavailable")
                    return
                }
            } ?: run {
                Log.w(TAG, "Nemotron model directory not found — offline ASR unavailable")
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot check for Nemotron model: ${e.message}")
            return
        }

        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = sampleRate, featureDim = 80),
            modelConfig = OnlineModelConfig(
                transducer = OnlineTransducerModelConfig(
                    encoder = "$modelDir/encoder.int8.onnx",
                    decoder = "$modelDir/decoder.int8.onnx",
                    joiner = "$modelDir/joiner.int8.onnx",
                ),
                tokens = "$modelDir/tokens.txt",
                numThreads = 4,
                debug = false,
                provider = "cpu",
            ),
            endpointConfig = EndpointConfig(
                rule1 = EndpointRule(false, 3.0f, 0.0f),
                rule2 = EndpointRule(true, 2.4f, 0.0f),
                rule3 = EndpointRule(false, 0.0f, 90.0f),
            ),
            enableEndpoint = true,
            decodingMethod = "greedy_search",
        )
        recognizer = OnlineRecognizer(assetManager = assetManager, config = config)
        stream = recognizer!!.createStream()
        Log.i(TAG, "Streaming ASR initialized (Nemotron 0.6B)")
    }

    override fun feedAudio(samples: FloatArray) {
        val s = stream ?: return
        s.acceptWaveform(samples, sampleRate)

        val rec = recognizer ?: return
        while (rec.isReady(s)) {
            rec.decode(s)
        }

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

        rec.reset(s)
        _partialResult.value = ""

        if (text.isEmpty()) return ""

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
