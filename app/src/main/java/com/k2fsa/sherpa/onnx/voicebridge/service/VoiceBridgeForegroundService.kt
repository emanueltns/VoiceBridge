package com.k2fsa.sherpa.onnx.voicebridge.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "VBService"

@AndroidEntryPoint
class VoiceBridgeForegroundService : Service() {

    @Inject lateinit var pipelineManager: AudioPipelineManager
    @Inject lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val pipelineState: StateFlow<PipelineState>
        get() = pipelineManager.pipelineState

    val currentConversationId: StateFlow<String?>
        get() = pipelineManager.currentConversationId

    val partialResult: StateFlow<String>
        get() = pipelineManager.partialResult

    val audioAmplitude: StateFlow<Float>
        get() = pipelineManager.audioAmplitude

    val isMuted: StateFlow<Boolean>
        get() = pipelineManager.isMuted

    fun setMuted(muted: Boolean) {
        pipelineManager.setMuted(muted)
    }

    fun setVoiceId(sid: Int) {
        pipelineManager.setVoiceId(sid)
    }

    fun sendTextMessage(text: String) {
        pipelineManager.sendTextMessage(text)
    }

    fun setAsrEngine(useAndroid: Boolean) {
        pipelineManager.setAsrEngine(useAndroid)
    }

    inner class LocalBinder : Binder() {
        val service: VoiceBridgeForegroundService
            get() = this@VoiceBridgeForegroundService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ServiceCommand.ACTION_START -> handleStart(intent)
            ServiceCommand.ACTION_STOP -> handleStop()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        val conversationId = intent.getStringExtra("conversation_id") ?: return

        val notification = notificationHelper.buildNotification(PipelineState.INITIALIZING)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }

        serviceScope.launch(Dispatchers.IO) {
            // initialize() is idempotent -- skips if already ready
            pipelineManager.initialize()
            pipelineManager.start(conversationId)
        }

        // Observe pipeline state for notification updates
        serviceScope.launch {
            pipelineManager.pipelineState.collect { state ->
                notificationHelper.updateNotification(state)
            }
        }

        Log.i(TAG, "Service started for conversation: $conversationId")
    }

    private fun handleStop() {
        pipelineManager.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "Service stopped")
    }

    override fun onDestroy() {
        // Don't release pipeline -- it's a singleton, stays warm for next call
        pipelineManager.stop()
        serviceScope.cancel()
        super.onDestroy()
    }
}
