package com.k2fsa.sherpa.onnx.voicebridge

import android.app.Application
import com.k2fsa.sherpa.onnx.voicebridge.service.AudioPipelineManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VoiceBridgeApp : Application() {

    @Inject lateinit var pipelineManager: AudioPipelineManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Pre-load ASR + TTS models on app startup so calls start instantly
        appScope.launch {
            pipelineManager.initialize()
        }
    }
}
