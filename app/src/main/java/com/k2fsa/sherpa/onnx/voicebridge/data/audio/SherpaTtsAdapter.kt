package com.k2fsa.sherpa.onnx.voicebridge.data.audio

import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
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

        val config = getOfflineTtsConfig(
            modelDir = modelDir,
            modelName = "model.onnx",
            acousticModelName = "",
            vocoder = "",
            voices = "voices.bin",
            lexicon = "",
            dataDir = "$dataDir/$modelDir/espeak-ng-data",
            dictDir = "",
            ruleFsts = "",
            ruleFars = "",
        )

        tts = OfflineTts(assetManager = assetManager, config = config)
        initAudioTrack()
    }

    override suspend fun speak(text: String) {
        val t = tts ?: return
        val track = audioTrack ?: return

        shouldStop = false
        _isSpeaking.value = true
        track.play()

        t.generateWithConfigAndCallback(
            text = text,
            config = GenerationConfig(sid = 0, speed = 1.0f),
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
        Thread.sleep(200)
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
