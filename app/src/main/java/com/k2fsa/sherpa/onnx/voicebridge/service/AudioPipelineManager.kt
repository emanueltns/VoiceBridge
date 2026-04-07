package com.k2fsa.sherpa.onnx.voicebridge.service

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.k2fsa.sherpa.onnx.voicebridge.data.audio.AudioRecordManager
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.AudioSegment
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.MessageRole
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.ConversationRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.VpsRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.SpeechRecognitionService
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.TextToSpeechService
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.VoiceActivityDetector
import com.k2fsa.sherpa.onnx.voicebridge.domain.usecase.GetIdleEntertainmentUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "AudioPipeline"
private const val ENTERTAINMENT_DELAY_MS = 3000L

class AudioPipelineManager @Inject constructor(
    private val vad: VoiceActivityDetector,
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

    private var captureJob: Job? = null
    private var processingJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Channel for passing speech segments from capture to processing
    private val segmentChannel = Channel<AudioSegment>(Channel.BUFFERED)

    private var toneGenerator: ToneGenerator? = null

    fun initialize() {
        _pipelineState.value = PipelineState.INITIALIZING
        Log.i(TAG, "Initializing models...")
        vad.initialize()
        asr.initialize()
        tts.initialize()
        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (_: Exception) { null }
        Log.i(TAG, "All models initialized")
    }

    fun start(conversationId: String) {
        _currentConversationId.value = conversationId

        if (!audioRecordManager.start()) {
            Log.e(TAG, "Failed to start audio recording")
            return
        }

        vad.reset()
        _pipelineState.value = PipelineState.LISTENING
        playTone(ToneGenerator.TONE_PROP_BEEP)

        // Capture runs independently — always reading from mic and feeding VAD
        captureJob = scope.launch { captureLoop() }

        // Processing picks up segments from the channel
        processingJob = scope.launch { processingLoop() }
    }

    fun stop() {
        captureJob?.cancel()
        processingJob?.cancel()
        captureJob = null
        processingJob = null
        audioRecordManager.stop()
        tts.stop()
        vad.reset()
        _pipelineState.value = PipelineState.IDLE
        _currentConversationId.value = null
    }

    fun release() {
        stop()
        vad.release()
        asr.release()
        tts.release()
        toneGenerator?.release()
        toneGenerator = null
    }

    /**
     * Capture loop: always reads audio from the mic and feeds VAD.
     * When speech is detected, sends the segment to the processing channel.
     * This loop NEVER blocks on transcription/network/TTS.
     */
    private suspend fun captureLoop() {
        val buffer = ShortArray(audioRecordManager.bufferSize)

        while (scope.isActive) {
            val ret = audioRecordManager.read(buffer)
            if (ret <= 0) {
                if (ret < 0) {
                    Log.w(TAG, "AudioRecord read error: $ret, recovering")
                    delay(500)
                    if (!audioRecordManager.start()) {
                        delay(2000)
                    } else {
                        vad.reset()
                    }
                }
                continue
            }

            val samples = FloatArray(ret) { buffer[it] / 32768.0f }

            try {
                vad.feedAudio(samples)
            } catch (e: Exception) {
                Log.e(TAG, "VAD error: ${e.message}, resetting")
                vad.reset()
                continue
            }

            while (vad.hasSpeechSegment()) {
                val segment = vad.getSpeechSegment()
                segmentChannel.trySend(segment)
            }
        }
    }

    /**
     * Processing loop: picks up speech segments and runs the full pipeline.
     * While this is busy (transcribing/sending/speaking), the capture loop
     * keeps running and buffering new segments.
     */
    private suspend fun processingLoop() {
        for (segment in segmentChannel) {
            if (!scope.isActive) break

            // Drain any extra segments that arrived while we were busy — use the latest one
            var latestSegment = segment
            while (true) {
                val next = segmentChannel.tryReceive().getOrNull() ?: break
                // Merge: concatenate samples for a more complete utterance
                val merged = FloatArray(latestSegment.samples.size + next.samples.size)
                latestSegment.samples.copyInto(merged)
                next.samples.copyInto(merged, latestSegment.samples.size)
                latestSegment = AudioSegment(merged)
            }

            playTone(ToneGenerator.TONE_PROP_ACK)
            _pipelineState.value = PipelineState.TRANSCRIBING
            val text = asr.transcribe(latestSegment)

            if (text.isBlank()) {
                _pipelineState.value = PipelineState.LISTENING
                playTone(ToneGenerator.TONE_PROP_BEEP)
                continue
            }

            val conversationId = _currentConversationId.value ?: continue

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
                tts.speak("I couldn't reach the server. I'll keep trying.")
            }

            _pipelineState.value = PipelineState.LISTENING
            playTone(ToneGenerator.TONE_PROP_BEEP)
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

            // Continuous entertainment loop
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

            // Let current TTS finish, then transition
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
}
