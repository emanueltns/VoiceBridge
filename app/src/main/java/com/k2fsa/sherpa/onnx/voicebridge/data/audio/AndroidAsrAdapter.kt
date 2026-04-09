package com.k2fsa.sherpa.onnx.voicebridge.data.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.k2fsa.sherpa.onnx.voicebridge.domain.service.SpeechRecognitionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

private const val TAG = "AndroidASR"
private const val RESTART_DELAY_MS = 500L

/**
 * Pause-tolerant endpoint: when SpeechRecognizer fires onResults(),
 * we DON'T immediately trigger the endpoint. Instead we buffer the text
 * and restart listening. If the user continues speaking within MERGE_WINDOW_MS,
 * the new text is appended. Only after MERGE_WINDOW_MS of silence
 * (no new onResults) do we fire the endpoint with the full accumulated text.
 */
private const val MERGE_WINDOW_MS = 2400L

class AndroidAsrAdapter @Inject constructor(
    private val context: Context,
) : SpeechRecognitionService {

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var isListening = false
    @Volatile private var isInitialized = false
    @Volatile private var shouldBeListening = false

    private val _partialResult = MutableStateFlow("")
    override val partialResult: StateFlow<String> = _partialResult.asStateFlow()

    // Buffered text accumulator (survives across SpeechRecognizer sessions)
    private val textBuffer = StringBuilder()
    private var endpointRunnable: Runnable? = null

    private var pendingFinalResult: String? = null
    @Volatile private var endpointDetected = false

    override fun initialize() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available on this device")
            return
        }
        mainHandler.post {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(createListener())
            isInitialized = true
            Log.i(TAG, "Android SpeechRecognizer initialized")
        }
    }

    override fun feedAudio(samples: FloatArray) {
        if (shouldBeListening && !isListening && !endpointDetected && isInitialized) {
            startListening()
        }
    }

    override fun isEndpoint(): Boolean {
        return endpointDetected
    }

    override fun getFinalResult(): String {
        val result = pendingFinalResult ?: ""
        pendingFinalResult = null
        endpointDetected = false
        _partialResult.value = ""
        textBuffer.clear()
        return result
    }

    override fun reset() {
        cancelEndpointTimer()
        pendingFinalResult = null
        endpointDetected = false
        _partialResult.value = ""
        textBuffer.clear()
        shouldBeListening = true
    }

    override fun release() {
        shouldBeListening = false
        isInitialized = false
        cancelEndpointTimer()
        mainHandler.post {
            try {
                recognizer?.stopListening()
                recognizer?.destroy()
            } catch (_: Exception) {}
            recognizer = null
            isListening = false
        }
    }

    private fun startListening() {
        if (isListening) return
        mainHandler.post {
            if (isListening) return@post
            val rec = recognizer ?: return@post

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                // Try to extend silence detection (supported on some devices)
                putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 3000L)
                putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 2500L)
                putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 1000L)
            }

            try {
                rec.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening: ${e.message}")
                isListening = false
                scheduleRestart()
            }
        }
    }

    private fun stopListening() {
        mainHandler.post {
            try {
                recognizer?.stopListening()
            } catch (_: Exception) {}
            isListening = false
        }
    }

    private fun scheduleRestart() {
        if (!shouldBeListening) return
        mainHandler.postDelayed({
            if (shouldBeListening && !isListening && !endpointDetected) {
                startListening()
            }
        }, RESTART_DELAY_MS)
    }

    private fun cancelEndpointTimer() {
        endpointRunnable?.let { mainHandler.removeCallbacks(it) }
        endpointRunnable = null
    }

    /**
     * Called when SpeechRecognizer delivers a final result.
     * Instead of immediately triggering the endpoint, buffer the text
     * and start a timer. If no new results arrive within MERGE_WINDOW_MS,
     * fire the endpoint. If the user keeps talking, the timer resets.
     */
    private fun onFinalChunk(text: String) {
        if (text.isBlank()) return

        // Append to buffer with a space separator
        if (textBuffer.isNotEmpty()) textBuffer.append(" ")
        textBuffer.append(text)

        // Show the accumulated text as partial result
        _partialResult.value = textBuffer.toString()
        Log.i(TAG, "Buffered: $textBuffer")

        // Cancel previous endpoint timer
        cancelEndpointTimer()

        // Start new timer — if user stays silent for MERGE_WINDOW_MS, fire endpoint
        val runnable = Runnable {
            val fullText = textBuffer.toString().trim()
            if (fullText.isNotBlank()) {
                Log.i(TAG, "Endpoint fired: $fullText")
                pendingFinalResult = fullText
                endpointDetected = true
                _partialResult.value = fullText
            }
        }
        endpointRunnable = runnable
        mainHandler.postDelayed(runnable, MERGE_WINDOW_MS)

        // Restart listening immediately to capture more speech
        isListening = false
        scheduleRestart()
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            // Don't cancel timer here — onBeginningOfSpeech fires from ambient noise too.
            // Timer only gets cancelled when we receive actual new text (in onPartialResults).
            Log.d(TAG, "Speech started")
        }

        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "Speech ended")
            isListening = false
        }

        override fun onError(error: Int) {
            isListening = false
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    Log.d(TAG, "No speech detected, restarting...")
                    scheduleRestart()
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    mainHandler.postDelayed({ scheduleRestart() }, 2000)
                }
                SpeechRecognizer.ERROR_CLIENT -> {
                    mainHandler.postDelayed({ scheduleRestart() }, 2000)
                }
                else -> {
                    Log.w(TAG, "Recognition error: $error")
                    scheduleRestart()
                }
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim() ?: ""

            if (text.isNotBlank()) {
                // Don't trigger endpoint immediately — buffer and wait
                onFinalChunk(text)
            } else {
                scheduleRestart()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim() ?: ""
            if (text.isNotBlank()) {
                // User is genuinely speaking new words — cancel the endpoint timer
                cancelEndpointTimer()

                val display = if (textBuffer.isNotEmpty()) {
                    "${textBuffer} $text"
                } else {
                    text
                }
                _partialResult.value = display
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
