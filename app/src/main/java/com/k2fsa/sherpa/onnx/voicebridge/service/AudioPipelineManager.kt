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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "AudioPipeline"
private const val ENTERTAINMENT_DELAY_MS = 3000L

@Singleton
class AudioPipelineManager @Inject constructor(
    @Named("sherpa") private val sherpaAsr: SpeechRecognitionService,
    @Named("android") private val androidAsr: SpeechRecognitionService,
    private val tts: TextToSpeechService,
    private val audioRecordManager: AudioRecordManager,
    private val conversationRepository: ConversationRepository,
    private val vpsRepository: VpsRepository,
    private val entertainmentUseCase: GetIdleEntertainmentUseCase,
) {
    // Active ASR engine (switchable at runtime)
    private var asr: SpeechRecognitionService = sherpaAsr
    private var useAndroidAsr = false

    @Volatile
    var funFactsEnabled = true

    fun setAsrEngine(useAndroid: Boolean) {
        if (useAndroid == useAndroidAsr) return
        val wasRunning = _pipelineState.value != PipelineState.IDLE
        if (wasRunning) asr.reset()

        useAndroidAsr = useAndroid
        asr = if (useAndroid) androidAsr else sherpaAsr
        Log.i(TAG, "ASR engine switched to: ${if (useAndroid) "Android" else "Sherpa (Nemotron)"}")
    }
    private val _pipelineState = MutableStateFlow(PipelineState.IDLE)
    val pipelineState: StateFlow<PipelineState> = _pipelineState.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    // Own StateFlow so observers don't break when ASR engine switches
    private val _partialResult = MutableStateFlow("")
    val partialResult: StateFlow<String> = _partialResult.asStateFlow()

    // Streaming response text (shows Claude's reply as it arrives)
    private val _streamingResponse = MutableStateFlow("")
    val streamingResponse: StateFlow<String> = _streamingResponse.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private var smoothedAmplitude = 0f

    private var pipelineJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Guards VPS sends — only one of processEndpoint / sendTextMessage may run at a time. */
    private val vpsSendMutex = Mutex()

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
        sherpaAsr.initialize()
        androidAsr.initialize()
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

        if (!useAndroidAsr) {
            // Sherpa needs AudioRecordManager for mic input
            if (!audioRecordManager.start()) {
                Log.e(TAG, "Failed to start audio recording")
                return
            }
        }
        // Android ASR manages its own mic — don't start AudioRecordManager
        // (they fight over the mic and SpeechRecognizer gets nothing)

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
            // Immediately kill any pending ASR endpoint timers (e.g. AndroidAsrAdapter's
            // 2.4s merge window) BEFORE acquiring the mutex, so a late timer can't sneak
            // a stale voice message into processEndpoint() while we wait for the lock.
            val wasMuted = _isMuted.value
            _isMuted.value = true
            asr.reset()
            _partialResult.value = ""

            vpsSendMutex.withLock {
                try {
                    conversationRepository.addMessage(conversationId, MessageRole.USER, text)

                    _pipelineState.value = PipelineState.SENDING
                    _streamingResponse.value = ""

                    val response = withContext(Dispatchers.IO) {
                        vpsRepository.sendMessageStreaming(text) { accumulated ->
                            _streamingResponse.value = accumulated
                        }
                    }
                    val responseText = response.getOrNull()

                    if (responseText != null) {
                        conversationRepository.addMessage(conversationId, MessageRole.ASSISTANT, responseText)
                        _pipelineState.value = PipelineState.SPEAKING
                        _streamingResponse.value = ""
                        tts.speak(responseText)
                    } else {
                        conversationRepository.addMessage(
                            conversationId, MessageRole.SYSTEM, "Failed to reach VPS",
                        )
                        _pipelineState.value = PipelineState.SPEAKING
                        _streamingResponse.value = ""
                        speakCue("I couldn't reach the server.")
                    }
                } finally {
                    _isMuted.value = wasMuted
                    _streamingResponse.value = ""

                    // Clean restart for next voice input
                    asr.reset()
                    _partialResult.value = ""
                    delay(300)

                    _pipelineState.value = PipelineState.LISTENING
                    playTone(ToneGenerator.TONE_PROP_BEEP)
                }
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

    private suspend fun streamingLoop() {
        if (useAndroidAsr) {
            androidAsrLoop()
        } else {
            sherpaAsrLoop()
        }
    }

    /**
     * Loop for Android SpeechRecognizer: polls for results.
     * No AudioRecordManager — SpeechRecognizer has exclusive mic access.
     * Orb amplitude comes from partial result changes instead.
     */
    private suspend fun androidAsrLoop() {
        var lastPartial = ""

        while (scope.isActive) {
            if (_isMuted.value) {
                _audioAmplitude.value = 0f
                delay(100)
                continue
            }

            // Trigger Android ASR to keep listening
            asr.feedAudio(FloatArray(0))

            // Sync partial results from ASR into our own flow
            val currentPartial = asr.partialResult.value
            _partialResult.value = currentPartial

            // Fake amplitude from partial result changes (orb reacts to new words)
            if (currentPartial != lastPartial && currentPartial.isNotBlank()) {
                _audioAmplitude.value = 0.7f
                lastPartial = currentPartial
            } else {
                _audioAmplitude.value = (_audioAmplitude.value * 0.85f).coerceAtLeast(0f)
            }

            if (currentPartial.isNotBlank() && _pipelineState.value == PipelineState.LISTENING) {
                _pipelineState.value = PipelineState.TRANSCRIBING
            }

            if (asr.isEndpoint()) {
                processEndpoint()
            }

            delay(80)
        }
    }

    /**
     * Loop for Sherpa (Nemotron): feeds raw audio to the on-device model.
     */
    private suspend fun sherpaAsrLoop() {
        val buffer = ShortArray(audioRecordManager.bufferSize)

        while (scope.isActive) {
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

            // Sync partial results
            _partialResult.value = asr.partialResult.value

            if (asr.partialResult.value.isNotBlank() && _pipelineState.value == PipelineState.LISTENING) {
                _pipelineState.value = PipelineState.TRANSCRIBING
            }

            if (asr.isEndpoint()) {
                processEndpoint()
            }
        }
    }

    private suspend fun processEndpoint() {
        val text = asr.getFinalResult()

        if (text.isBlank()) {
            _pipelineState.value = PipelineState.LISTENING
            return
        }

        Log.i(TAG, "Transcribed: $text")

        // If the pipeline is already busy (e.g. a text message grabbed the lock),
        // discard this late voice endpoint — the user switched to typed input.
        if (!vpsSendMutex.tryLock()) {
            Log.w(TAG, "Dropping voice endpoint — pipeline busy with another send")
            asr.reset()
            _partialResult.value = ""
            return
        }

        try {
            // Re-check state after acquiring lock — sendTextMessage may have just finished
            val state = _pipelineState.value
            if (state == PipelineState.SENDING || state == PipelineState.SPEAKING) {
                Log.w(TAG, "Dropping voice endpoint — pipeline state is $state")
                return
            }

            val conversationId = _currentConversationId.value ?: return

            playTone(ToneGenerator.TONE_PROP_ACK)
            conversationRepository.addMessage(conversationId, MessageRole.USER, text)

            _pipelineState.value = PipelineState.SENDING
            _streamingResponse.value = ""

            // Launch streaming VPS call + optional entertainment in parallel
            val responseText = withContext(Dispatchers.IO) {
                val responseDeferred = async {
                    vpsRepository.sendMessageStreaming(text) { accumulated ->
                        _streamingResponse.value = accumulated
                    }
                }

                // Entertainment: speak fun facts if response takes > 3s
                val entertainmentJob = if (funFactsEnabled) {
                    launch {
                        delay(ENTERTAINMENT_DELAY_MS)
                        while (isActive && !responseDeferred.isCompleted) {
                            _pipelineState.value = PipelineState.ENTERTAINING
                            val fact = entertainmentUseCase()
                            tts.speak(fact)
                            if (!responseDeferred.isCompleted) {
                                delay(1500)
                            }
                        }
                    }
                } else null

                val result = responseDeferred.await()
                entertainmentJob?.cancel()

                // If we were entertaining, let TTS finish and announce transition
                if (tts.isSpeaking.value) {
                    var waitCount = 0
                    while (tts.isSpeaking.value && waitCount < 100) {
                        delay(100)
                        waitCount++
                    }
                    tts.speak("Alright, I have the response now.")
                }

                result.getOrNull()
            }

            if (responseText != null) {
                conversationRepository.addMessage(conversationId, MessageRole.ASSISTANT, responseText)
                playTone(ToneGenerator.TONE_PROP_ACK)
                _pipelineState.value = PipelineState.SPEAKING
                _streamingResponse.value = ""
                tts.speak(responseText)
            } else {
                conversationRepository.addMessage(
                    conversationId, MessageRole.SYSTEM, "Failed to reach VPS",
                )
                _pipelineState.value = PipelineState.SPEAKING
                _streamingResponse.value = ""
                speakCue("I couldn't reach the server. Please check your VPS connection in settings.")
            }
        } finally {
            asr.reset()
            _partialResult.value = ""
            _pipelineState.value = PipelineState.LISTENING
            playTone(ToneGenerator.TONE_PROP_BEEP)
            vpsSendMutex.unlock()
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
