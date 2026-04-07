package com.k2fsa.sherpa.onnx.voicebridge.data.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import javax.inject.Inject

private const val TAG = "AudioRecordMgr"

class AudioRecordManager @Inject constructor() {

    var audioRecord: AudioRecord? = null
        private set

    val sampleRate = 16000
    val bufferSize = 512

    fun start(): Boolean {
        stop() // Release any existing recorder first
        return try {
            val numBytes = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )

            if (numBytes == AudioRecord.ERROR || numBytes == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size: $numBytes")
                return false
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                numBytes * 2,
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            val recording = audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING
            Log.i(TAG, "AudioRecord started: $recording")
            recording
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord: ${e.message}")
            audioRecord?.release()
            audioRecord = null
            false
        }
    }

    fun read(buffer: ShortArray): Int {
        val rec = audioRecord ?: return -1
        if (rec.recordingState != AudioRecord.RECORDSTATE_RECORDING) return -1
        return rec.read(buffer, 0, buffer.size)
    }

    fun stop() {
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }
}
