package com.k2fsa.sherpa.onnx.voicebridge.data.audio

import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.TextToSpeechService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class SherpaTtsAdapter @Inject constructor(
    private val assetManager: AssetManager,
    private val assetCopier: AssetCopier,
) : TextToSpeechService {

    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    @Volatile
    private var shouldStop = false

    override fun initialize() {
        val modelDir = "kokoro-en-v0_19"
        val dataDir = assetCopier.copyDataDir("$modelDir/espeak-ng-data")

        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(
                    model = "$modelDir/model.onnx",
                    voices = "$modelDir/voices.bin",
                    tokens = "$modelDir/tokens.txt",
                    dataDir = "$dataDir/$modelDir/espeak-ng-data",
                ),
                numThreads = 4,
                debug = false,
                provider = "cpu",
            ),
            maxNumSentences = 4,   // Process 4 sentences at once — fewer pauses
            silenceScale = 0.05f,  // Minimal inter-sentence silence
        )

        tts = OfflineTts(assetManager = assetManager, config = config)
        initAudioTrack()
    }

    override suspend fun speak(text: String) {
        val t = tts ?: return
        val track = audioTrack ?: return

        shouldStop = false
        _isSpeaking.value = true

        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            track.play()
        }

        t.generateWithConfigAndCallback(
            text = text,
            config = GenerationConfig(sid = 0, speed = 1.05f),
            callback = { samples ->
                if (!shouldStop) {
                    track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                    1
                } else {
                    0
                }
            },
        )

        // Brief pause for audio to finish draining
        Thread.sleep(150)
        _isSpeaking.value = false
    }

    override fun stop() {
        shouldStop = true
        audioTrack?.pause()
        audioTrack?.flush()
        _isSpeaking.value = false
    }

    override fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
        tts?.free()
        tts = null
    }

    private fun initAudioTrack() {
        val sampleRate = tts?.sampleRate() ?: return
        val bufLength = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )

        val attr = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setSampleRate(sampleRate)
            .build()

        audioTrack = AudioTrack(
            attr, format, bufLength, AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
    }
}
