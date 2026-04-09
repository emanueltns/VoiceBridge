package com.k2fsa.sherpa.onnx.voicebridge.service

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.k2fsa.sherpa.onnx.voicebridge.data.audio.AudioRecordManager
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.MessageRole
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.ConversationRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.VpsRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.SpeechRecognitionService
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.TextToSpeechService
import com.k2fsa.sherpa.onnx.voicebridge.domain.usecase.GetIdleEntertainmentUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AudioPipeline"
private const val ENTERTAINMENT_DELAY_MS = 3000L

@Singleton
class AudioPipelineManager @Inject constructor(
    private val asr: SpeechRecognitionService,
    private val tts: TextToSpeechService,
    private val audioRecordManager: AudioRecordManager,
    private val conversationRepository: ConversationRepository,
    private val vpsRepository: VpsRepository,
    private val entertainmentUseCase: GetIdleEntertainmentUseCase,
) {
    private val _pipelineState = MutableStateFlow(PipelineState.IDLE)
    val pipelineState: StateFlow<PipelineState> = _pipelineState.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    val partialResult: StateFlow<String> get() = asr.partialResult

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private var smoothedAmplitude = 0f

    private var pipelineJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var toneGenerator: ToneGenerator? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    @Volatile
    private var isInitializing = false

    // Mute support
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) {
            audioRecordManager.stop()
        } else if (_pipelineState.value != PipelineState.IDLE) {
            audioRecordManager.start()
            asr.reset()
            _pipelineState.value = PipelineState.LISTENING
        }
    }

    fun initialize() {
        if (_isReady.value || isInitializing) return
        isInitializing = true

        _pipelineState.value = PipelineState.INITIALIZING
        Log.i(TAG, "Initializing models...")
        asr.initialize()
        tts.initialize()
        Log.i(TAG, "TTS has ${tts.numSpeakers()} voices available")
        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (_: Exception) { null }

        _isReady.value = true
        _pipelineState.value = PipelineState.IDLE
        isInitializing = false
        Log.i(TAG, "All models initialized and ready")
    }

    fun setVoiceId(sid: Int) {
        tts.setSpeakerId(sid)
        Log.i(TAG, "Voice changed to speaker $sid")
    }

    fun start(conversationId: String) {
        _currentConversationId.value = conversationId

        if (!audioRecordManager.start()) {
            Log.e(TAG, "Failed to start audio recording")
            return
        }

        asr.reset()
        _pipelineState.value = PipelineState.LISTENING
        playTone(ToneGenerator.TONE_PROP_BEEP)

        pipelineJob = scope.launch { streamingLoop() }
    }

    /**
     * Send a typed text message through the pipeline (same flow as voice,
     * but bypasses ASR). Mutes mic while processing to avoid collision.
     */
    fun sendTextMessage(text: String) {
        val conversationId = _currentConversationId.value ?: return
        scope.launch {
            // Temporarily pause the mic so the streaming loop doesn't interfere
            val wasMuted = _isMuted.value
            _isMuted.value = true

            try {
                conversationRepository.addMessage(conversationId, MessageRole.USER, text)

                _pipelineState.value = PipelineState.SENDING
                val response = sendWithEntertainment(text)

                if (response != null) {
                    conversationRepository.addMessage(conversationId, MessageRole.ASSISTANT, response)
                    _pipelineState.value = PipelineState.SPEAKING
                    tts.speak(response)
                } else {
                    conversationRepository.addMessage(
                        conversationId, MessageRole.SYSTEM, "Failed to reach VPS",
                    )
                    _pipelineState.value = PipelineState.SPEAKING
                    speakCue("I couldn't reach the server.")
                }
            } finally {
                // Restore mic state and resume listening
                _isMuted.value = wasMuted
                asr.reset()
                _pipelineState.value = PipelineState.LISTENING
                playTone(ToneGenerator.TONE_PROP_BEEP)
            }
        }
    }

    fun stop() {
        pipelineJob?.cancel()
        pipelineJob = null
        audioRecordManager.stop()
        tts.stop()
        asr.reset()
        _pipelineState.value = PipelineState.IDLE
        _currentConversationId.value = null
    }

    fun release() {
        stop()
        asr.release()
        tts.release()
        toneGenerator?.release()
        toneGenerator = null
    }

    /**
     * Main streaming loop:
     * 1. Continuously reads audio and feeds it to the streaming ASR
     * 2. ASR processes in real-time, partial results update live
     * 3. When ASR detects an endpoint (natural pause), grab final text
     * 4. Send to VPS, entertain while waiting, speak response
     * 5. Resume listening immediately
     */
    private suspend fun streamingLoop() {
        val buffer = ShortArray(audioRecordManager.bufferSize)

        while (scope.isActive) {
            // Skip audio processing when muted
            if (_isMuted.value) {
                _audioAmplitude.value = 0f
                delay(100)
                continue
            }

            val ret = audioRecordManager.read(buffer)
            if (ret <= 0) {
                if (ret < 0) {
                    Log.w(TAG, "AudioRecord read error: $ret, recovering")
                    delay(500)
                    if (!audioRecordManager.start()) {
                        delay(2000)
                    }
                }
                continue
            }

            val samples = FloatArray(ret) { buffer[it] / 32768.0f }

            // Compute audio amplitude (RMS) with exponential smoothing
            var sumSquares = 0f
            for (s in samples) sumSquares += s * s
            val rms = kotlin.math.sqrt(sumSquares / samples.size)
            smoothedAmplitude = smoothedAmplitude * 0.7f + rms * 0.3f
            _audioAmplitude.value = (smoothedAmplitude * 8f).coerceIn(0f, 1f)

            try {
                asr.feedAudio(samples)
            } catch (e: Exception) {
                Log.e(TAG, "ASR feed error: ${e.message}")
                asr.reset()
                continue
            }

            // Check if ASR detected an endpoint (user stopped speaking)
            if (asr.isEndpoint()) {
                val text = asr.getFinalResult()

                if (text.isBlank()) {
                    _pipelineState.value = PipelineState.LISTENING
                    continue
                }

                Log.i(TAG, "Transcribed: $text")
                val conversationId = _currentConversationId.value ?: continue

                // Process the utterance
                playTone(ToneGenerator.TONE_PROP_ACK)
                conversationRepository.addMessage(conversationId, MessageRole.USER, text)

                _pipelineState.value = PipelineState.SENDING
                val response = sendWithEntertainment(text)

                if (response != null) {
                    conversationRepository.addMessage(conversationId, MessageRole.ASSISTANT, response)
                    playTone(ToneGenerator.TONE_PROP_ACK)
                    _pipelineState.value = PipelineState.SPEAKING
                    tts.speak(response)
                } else {
                    conversationRepository.addMessage(
                        conversationId, MessageRole.SYSTEM, "Failed to reach VPS",
                    )
                    _pipelineState.value = PipelineState.SPEAKING
                    speakCue("I couldn't reach the server. Please check your VPS connection in settings. I'll keep trying.")
                }

                // Back to listening — reset FIRST, then announce
                asr.reset()
                _pipelineState.value = PipelineState.LISTENING
                playTone(ToneGenerator.TONE_PROP_BEEP)
            } else if (asr.partialResult.value.isNotBlank()) {
                // We have partial text — show we're actively transcribing
                if (_pipelineState.value == PipelineState.LISTENING) {
                    _pipelineState.value = PipelineState.TRANSCRIBING
                }
            }
        }
    }

    private suspend fun sendWithEntertainment(text: String): String? {
        return withContext(Dispatchers.IO) {
            val responseDeferred = async { vpsRepository.sendMessage(text) }
            val responseHolder = CompletableDeferred<Result<String>?>()

            launch {
                try {
                    val result = responseDeferred.await()
                    responseHolder.complete(result)
                } catch (e: CancellationException) {
                    responseHolder.complete(null)
                } catch (e: Exception) {
                    responseHolder.complete(Result.failure(e))
                }
            }

            val entertainmentJob = launch {
                delay(ENTERTAINMENT_DELAY_MS)
                while (isActive && !responseHolder.isCompleted) {
                    _pipelineState.value = PipelineState.ENTERTAINING
                    val fact = entertainmentUseCase()
                    tts.speak(fact)
                    if (!responseHolder.isCompleted) {
                        delay(1500)
                    }
                }
            }

            val result = responseHolder.await()
            entertainmentJob.cancel()

            if (tts.isSpeaking.value) {
                var waitCount = 0
                while (tts.isSpeaking.value && waitCount < 100) {
                    delay(100)
                    waitCount++
                }
                tts.speak("Alright, I have the response now.")
            }

            result?.getOrNull()
        }
    }

    private fun playTone(toneType: Int) {
        try {
            toneGenerator?.startTone(toneType, 150)
        } catch (_: Exception) {}
    }

    /**
     * Speaks a short voice cue so the user knows what's happening
     * (essential for hands-free / driving use).
     */
    private suspend fun speakCue(text: String) {
        try {
            tts.speak(text)
        } catch (_: Exception) {}
    }
}
