package com.k2fsa.sherpa.onnx.voicebridge

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.getFeatureConfig
import com.k2fsa.sherpa.onnx.getOfflineModelConfig
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import com.k2fsa.sherpa.onnx.getVadModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread

private const val TAG = "VoiceBridge"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class LegacyMainActivity : AppCompatActivity() {

    // UI
    private lateinit var actionButton: Button
    private lateinit var statusText: TextView
    private lateinit var conversationText: TextView
    private lateinit var serverHostInput: EditText
    private lateinit var serverPortInput: EditText

    // VAD + ASR
    private lateinit var vad: Vad
    private lateinit var offlineRecognizer: OfflineRecognizer

    // TTS
    private lateinit var tts: OfflineTts
    private lateinit var audioTrack: AudioTrack

    // Audio recording
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val sampleRateInHz = 16000

    private val permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    @Volatile
    private var isRunning: Boolean = false

    @Volatile
    private var isSpeaking: Boolean = false

    private var conversationLog: String = ""

    // State machine
    enum class State { IDLE, LISTENING, TRANSCRIBING, SENDING, SPEAKING }

    @Volatile
    private var state: State = State.IDLE

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Audio record permission denied")
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        statusText = findViewById(R.id.status_text)
        conversationText = findViewById(R.id.conversation_text)
        conversationText.movementMethod = ScrollingMovementMethod()
        actionButton = findViewById(R.id.action_button)
        serverHostInput = findViewById(R.id.server_host)
        serverPortInput = findViewById(R.id.server_port)

        actionButton.isEnabled = false
        actionButton.setOnClickListener { onActionClick() }

        setStatus("Initializing models...")

        lifecycleScope.launch(Dispatchers.IO) {
            initVad()
            initAsr()
            initTts()
            initAudioTrack()

            withContext(Dispatchers.Main) {
                actionButton.isEnabled = true
                setStatus("Ready. Tap Start to begin.")
            }
        }
    }

    private fun setStatus(text: String) {
        statusText.text = text
    }

    private fun appendConversation(role: String, text: String) {
        conversationLog += "\n$role: $text\n"
        conversationText.text = conversationLog
        // Auto-scroll to bottom
        val scrollAmount = conversationText.layout?.let {
            it.getLineTop(conversationText.lineCount) - conversationText.height
        } ?: 0
        if (scrollAmount > 0) conversationText.scrollTo(0, scrollAmount)
    }

    private fun onActionClick() {
        if (!isRunning) {
            val host = serverHostInput.text.toString().trim()
            if (host.isEmpty()) {
                setStatus("Please enter the VPS Tailscale IP")
                return
            }
            startConversation()
        } else {
            stopConversation()
        }
    }

    private fun startConversation() {
        if (!initMicrophone()) return

        isRunning = true
        actionButton.setText(R.string.stop)
        audioRecord!!.startRecording()
        vad.reset()
        state = State.LISTENING
        setStatus("Listening...")

        recordingThread = thread(true) { conversationLoop() }
    }

    private fun stopConversation() {
        isRunning = false
        isSpeaking = false
        state = State.IDLE

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        audioTrack.pause()
        audioTrack.flush()

        runOnUiThread {
            actionButton.setText(R.string.start)
            setStatus("Stopped.")
        }
    }

    private fun conversationLoop() {
        val bufferSize = 512
        val buffer = ShortArray(bufferSize)

        while (isRunning) {
            val ret = audioRecord?.read(buffer, 0, buffer.size) ?: -1
            if (ret <= 0) continue

            val samples = FloatArray(ret) { buffer[it] / 32768.0f }
            vad.acceptWaveform(samples)

            while (!vad.empty() && isRunning) {
                val segment = vad.front()
                vad.pop()

                // Speech segment detected — transcribe it
                runOnUiThread { setStatus("Transcribing...") }
                state = State.TRANSCRIBING

                val text = transcribe(segment.samples)
                if (text.isBlank()) {
                    runOnUiThread { setStatus("Listening...") }
                    state = State.LISTENING
                    continue
                }

                runOnUiThread {
                    appendConversation("You", text)
                    setStatus("Sending to Claude...")
                }
                state = State.SENDING

                // Send to VPS and get response
                val host = serverHostInput.text.toString().trim()
                val port = serverPortInput.text.toString().trim().toIntOrNull() ?: 9999
                val response = sendToVps(host, port, text)

                if (response == null) {
                    runOnUiThread {
                        appendConversation("System", "Failed to reach VPS")
                        setStatus("Listening...")
                    }
                    state = State.LISTENING
                    continue
                }

                runOnUiThread {
                    appendConversation("Claude", response)
                    setStatus("Speaking...")
                }
                state = State.SPEAKING

                // Speak the response via TTS
                speakResponse(response)

                // Back to listening
                runOnUiThread { setStatus("Listening...") }
                state = State.LISTENING
                vad.reset()
            }
        }
    }

    private fun transcribe(samples: FloatArray): String {
        val stream = offlineRecognizer.createStream()
        stream.acceptWaveform(samples, sampleRateInHz)
        offlineRecognizer.decode(stream)
        val result = offlineRecognizer.getResult(stream)
        stream.release()
        return result.text.trim()
    }

    private fun sendToVps(host: String, port: Int, text: String): String? {
        return try {
            val socket = Socket(host, port)
            socket.soTimeout = 180_000 // 3 min timeout for Claude response

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            writer.print(text)
            writer.flush()
            socket.shutdownOutput()

            // Read until ---END--- marker
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line == "---END---") break
                sb.appendLine(line)
            }

            socket.close()
            sb.toString().trim()
        } catch (e: Exception) {
            Log.e(TAG, "VPS communication error: ${e.message}")
            null
        }
    }

    private fun speakResponse(text: String) {
        isSpeaking = true
        audioTrack.play()

        tts.generateWithConfigAndCallback(
            text = text,
            config = GenerationConfig(sid = 0, speed = 1.0f),
            callback = { samples ->
                if (isSpeaking && isRunning) {
                    audioTrack.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                    1
                } else {
                    0
                }
            }
        )

        // Wait for audio to finish playing
        Thread.sleep(200)
        isSpeaking = false
    }

    // --- Initialization ---

    private fun initVad() {
        Log.i(TAG, "Initializing VAD")
        val config = getVadModelConfig(0)!!
        vad = Vad(assetManager = application.assets, config = config)
        Log.i(TAG, "VAD initialized")
    }

    private fun initAsr() {
        Log.i(TAG, "Initializing ASR (Whisper tiny.en)")
        val config = OfflineRecognizerConfig(
            featConfig = getFeatureConfig(sampleRate = sampleRateInHz, featureDim = 80),
            modelConfig = getOfflineModelConfig(type = 2)!!, // whisper-tiny.en
        )
        offlineRecognizer = OfflineRecognizer(assetManager = application.assets, config = config)
        Log.i(TAG, "ASR initialized")
    }

    private fun initTts() {
        Log.i(TAG, "Initializing TTS (Kokoro en v0.19)")

        val modelDir = "kokoro-en-v0_19"
        val dataDir = copyDataDir("$modelDir/espeak-ng-data")

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
        )!!

        tts = OfflineTts(assetManager = application.assets, config = config)
        Log.i(TAG, "TTS initialized (Kokoro, sample rate: ${tts.sampleRate()})")
    }

    private fun initAudioTrack() {
        val sampleRate = tts.sampleRate()
        val bufLength = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
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
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    private fun initMicrophone(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
            return false
        }

        val numBytes = AudioRecord.getMinBufferSize(
            sampleRateInHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRateInHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            numBytes * 2
        )
        return true
    }

    // --- Asset copying for TTS data dir ---

    private fun copyDataDir(dataDir: String): String {
        copyAssets(dataDir)
        val extDir = application.getExternalFilesDir(null)
            ?: application.filesDir
        return extDir.absolutePath
    }

    private fun copyAssets(path: String) {
        try {
            val assets = application.assets.list(path)
            if (assets.isNullOrEmpty()) {
                copyFile(path)
            } else {
                val extDir = application.getExternalFilesDir(null) ?: application.filesDir
                val fullPath = "$extDir/$path"
                File(fullPath).mkdirs()
                for (asset in assets) {
                    val p = if (path.isEmpty()) "" else "$path/"
                    copyAssets(p + asset)
                }
            }
        } catch (ex: IOException) {
            Log.e(TAG, "Failed to copy $path: $ex")
        }
    }

    private fun copyFile(filename: String) {
        try {
            val istream = application.assets.open(filename)
            val extDir = application.getExternalFilesDir(null) ?: application.filesDir
            val newFilename = "$extDir/$filename"
            val ostream = java.io.FileOutputStream(newFilename)
            val buffer = ByteArray(1024)
            var read: Int
            while (istream.read(buffer).also { read = it } != -1) {
                ostream.write(buffer, 0, read)
            }
            istream.close()
            ostream.flush()
            ostream.close()
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to copy $filename: $ex")
        }
    }
}
