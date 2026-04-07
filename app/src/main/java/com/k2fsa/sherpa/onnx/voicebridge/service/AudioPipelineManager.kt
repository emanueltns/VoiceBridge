package com.k2fsa.sherpa.onnx.voicebridge.service

import android.util.Log
import com.k2fsa.sherpa.onnx.voicebridge.data.audio.AudioRecordManager
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.MessageRole
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.ConversationRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.VpsRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.SpeechRecognitionService
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.TextToSpeechService
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.VoiceActivityDetector
import com.k2fsa.sherpa.onnx.voicebridge.domain.usecase.GetIdleEntertainmentUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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

    private var pipelineJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize() {
        _pipelineState.value = PipelineState.INITIALIZING
        Log.i(TAG, "Initializing models...")
        vad.initialize()
        asr.initialize()
        tts.initialize()
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

        pipelineJob = scope.launch {
            conversationLoop()
        }
    }

    fun stop() {
        pipelineJob?.cancel()
        pipelineJob = null
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
    }

    private suspend fun conversationLoop() {
        val buffer = ShortArray(audioRecordManager.bufferSize)

        while (scope.isActive) {
            val ret = audioRecordManager.read(buffer)
            if (ret <= 0) continue

            val samples = FloatArray(ret) { buffer[it] / 32768.0f }
            vad.feedAudio(samples)

            while (vad.hasSpeechSegment() && scope.isActive) {
                val segment = vad.getSpeechSegment()

                // Transcribe
                _pipelineState.value = PipelineState.TRANSCRIBING
                val text = asr.transcribe(segment)

                if (text.isBlank()) {
                    _pipelineState.value = PipelineState.LISTENING
                    continue
                }

                val conversationId = _currentConversationId.value ?: continue

                // Save user message
                conversationRepository.addMessage(conversationId, MessageRole.USER, text)

                // Send to VPS with entertainment while waiting
                _pipelineState.value = PipelineState.SENDING

                val response = sendWithEntertainment(text)

                if (response != null) {
                    // Save assistant message
                    conversationRepository.addMessage(conversationId, MessageRole.ASSISTANT, response)

                    // Speak response
                    _pipelineState.value = PipelineState.SPEAKING
                    tts.speak(response)
                } else {
                    conversationRepository.addMessage(
                        conversationId, MessageRole.SYSTEM, "Failed to reach VPS",
                    )
                }

                // Back to listening
                _pipelineState.value = PipelineState.LISTENING
                vad.reset()
            }
        }
    }

    private suspend fun sendWithEntertainment(text: String): String? {
        return withContext(Dispatchers.IO) {
            val responseDeferred = async { vpsRepository.sendMessage(text) }

            // Start entertainment timer
            val entertainmentJob = launch {
                delay(ENTERTAINMENT_DELAY_MS)
                if (responseDeferred.isActive) {
                    _pipelineState.value = PipelineState.ENTERTAINING
                    val joke = entertainmentUseCase()
                    tts.speak(joke)
                    // If VPS still hasn't responded, go back to waiting
                    if (responseDeferred.isActive) {
                        _pipelineState.value = PipelineState.SENDING
                    }
                }
            }

            try {
                val result = responseDeferred.await()
                entertainmentJob.cancel()
                tts.stop() // Stop any entertainment TTS
                result.getOrNull()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "VPS error: ${e.message}")
                null
            }
        }
    }
}
