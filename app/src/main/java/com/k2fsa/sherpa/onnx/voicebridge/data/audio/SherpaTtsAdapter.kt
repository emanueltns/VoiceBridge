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

    @Volatile
    private var currentSid = 0

    private val speakLock = Any()

    override fun initialize() {
        val modelDir = "kokoro-multi-lang-v1_0"
        val dataDir = assetCopier.copyDataDir("$modelDir/espeak-ng-data")

        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(
                    model = "$modelDir/model.onnx",
                    voices = "$modelDir/voices.bin",
                    tokens = "$modelDir/tokens.txt",
                    dataDir = "$dataDir/$modelDir/espeak-ng-data",
                    lang = "en",
                ),
                numThreads = 4,
                debug = false,
                provider = "cpu",
            ),
            maxNumSentences = 1,
            silenceScale = 0.05f,
        )

        tts = OfflineTts(assetManager = assetManager, config = config)
        initAudioTrack()
    }

    override fun setSpeakerId(sid: Int) {
        currentSid = sid
    }

    override fun numSpeakers(): Int {
        return tts?.numSpeakers() ?: 1
    }

    override suspend fun speak(text: String) {
        val t = tts ?: return
        val track = audioTrack ?: return

        synchronized(speakLock) {
            shouldStop = false
            _isSpeaking.value = true

            try {
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track.play()
                }

                val sentences = splitSentences(text)
                if (sentences.isEmpty()) return

                // Pre-generate first chunk
                var nextAudio = generateChunk(t, sentences[0])

                for (i in sentences.indices) {
                    if (shouldStop) break

                    val currentAudio = nextAudio ?: continue

                    // Start generating NEXT chunk in background while current plays
                    val nextSentence = sentences.getOrNull(i + 1)
                    var futureAudio: FloatArray? = null
                    val genThread = if (nextSentence != null && !shouldStop) {
                        Thread {
                            futureAudio = generateChunk(t, nextSentence)
                        }.also { it.start() }
                    } else null

                    // Play current chunk (blocks until audio is written)
                    writeAudio(track, currentAudio)

                    // Wait for next chunk generation to finish
                    genThread?.join()
                    nextAudio = futureAudio
                }

                Thread.sleep(150)
            } catch (e: Exception) {
                android.util.Log.e("SherpaTts", "TTS speak error: ${e.message}", e)
            } finally {
                _isSpeaking.value = false
            }
        }
    }

    /**
     * Generate audio samples for a text chunk (blocking).
     */
    private fun generateChunk(t: OfflineTts, text: String): FloatArray? {
        if (shouldStop || text.isBlank()) return null
        return try {
            val result = t.generateWithConfig(
                text = text,
                config = GenerationConfig(sid = currentSid, speed = 1.0f),
            )
            result.samples
        } catch (e: Exception) {
            android.util.Log.e("SherpaTts", "Generate error: ${e.message}")
            null
        }
    }

    /**
     * Write pre-generated audio samples to AudioTrack.
     */
    private fun writeAudio(track: AudioTrack, samples: FloatArray) {
        var offset = 0
        val chunkSize = 4096
        while (offset < samples.size && !shouldStop) {
            val len = minOf(chunkSize, samples.size - offset)
            track.write(samples, offset, len, AudioTrack.WRITE_BLOCKING)
            offset += len
        }
    }

    /**
     * Splits text into small chunks for smooth TTS playback.
     * Each chunk = one sentence. Long sentences split on commas/semicolons.
     * Max ~80 chars per chunk to keep generation fast and speech natural.
     */
    private fun splitSentences(text: String): List<String> {
        // First split on sentence boundaries
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        val result = mutableListOf<String>()

        for (sentence in sentences) {
            val trimmed = sentence.trim()
            if (trimmed.isBlank()) continue

            if (trimmed.length <= 80) {
                result.add(trimmed)
            } else {
                // Break long sentences on commas, semicolons, colons, dashes
                val parts = trimmed.split(Regex("(?<=[,;:\\-])\\s+"))
                val buffer = StringBuilder()
                for (part in parts) {
                    if (buffer.length + part.length > 80 && buffer.isNotEmpty()) {
                        result.add(buffer.toString().trim())
                        buffer.clear()
                    }
                    if (buffer.isNotEmpty()) buffer.append(" ")
                    buffer.append(part)
                }
                if (buffer.isNotBlank()) {
                    result.add(buffer.toString().trim())
                }
            }
        }
        return result
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
