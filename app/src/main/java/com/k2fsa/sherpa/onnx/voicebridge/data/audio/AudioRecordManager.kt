package com.k2fsa.sherpa.onnx.voicebridge.data.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import javax.inject.Inject

class AudioRecordManager @Inject constructor() {

    var audioRecord: AudioRecord? = null
        private set

    val sampleRate = 16000
    val bufferSize = 512

    fun start(): Boolean {
        val numBytes = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            numBytes * 2,
        )

        audioRecord?.startRecording()
        return audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING
    }

    fun read(buffer: ShortArray): Int {
        return audioRecord?.read(buffer, 0, buffer.size) ?: -1
    }

    fun stop() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
