package com.k2fsa.sherpa.onnx.voicebridge.data.audio

import android.content.res.AssetManager
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.getFeatureConfig
import com.k2fsa.sherpa.onnx.getOfflineModelConfig
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.AudioSegment
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.SpeechRecognitionService
import javax.inject.Inject

private const val TAG = "SherpaASR"

// Whisper hallucination patterns — these appear when fed silence or noise
private val HALLUCINATION_PATTERNS = listOf(
    "thank you",
    "thanks for watching",
    "subscribe",
    "like and subscribe",
    "please subscribe",
    "see you next time",
    "bye bye",
    "goodbye",
    "you",
    "the end",
    "so",
    "uh",
    "um",
    "hmm",
    "oh",
    "ah",
)

class SherpaAsrAdapter @Inject constructor(
    private val assetManager: AssetManager,
) : SpeechRecognitionService {

    private var recognizer: OfflineRecognizer? = null
    private val sampleRate = 16000

    override fun initialize() {
        val config = OfflineRecognizerConfig(
            featConfig = getFeatureConfig(sampleRate = sampleRate, featureDim = 80),
            modelConfig = getOfflineModelConfig(type = 2)!!, // whisper-tiny.en
        )
        recognizer = OfflineRecognizer(assetManager = assetManager, config = config)
    }

    override fun transcribe(segment: AudioSegment): String {
        val rec = recognizer ?: return ""

        // Skip very short segments (less than 0.5s at 16kHz)
        if (segment.samples.size < sampleRate / 2) {
            Log.d(TAG, "Skipping short segment: ${segment.samples.size} samples")
            return ""
        }

        val stream = rec.createStream()
        stream.acceptWaveform(segment.samples, sampleRate)
        rec.decode(stream)
        val result = rec.getResult(stream)
        stream.release()

        val text = result.text.trim()

        // Filter out Whisper hallucinations
        val lower = text.lowercase()
        if (lower.isEmpty()) return ""
        if (HALLUCINATION_PATTERNS.any { lower == it || lower == "$it." }) {
            Log.d(TAG, "Filtered hallucination: $text")
            return ""
        }
        // Very short results (1-2 words) on short audio are usually noise
        if (text.split(" ").size <= 2 && segment.samples.size < sampleRate * 2) {
            Log.d(TAG, "Filtered short result on short audio: $text")
            return ""
        }

        return text
    }

    override fun release() {
        recognizer?.release()
        recognizer = null
    }
}
