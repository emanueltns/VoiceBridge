package com.k2fsa.sherpa.onnx.voicebridge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.k2fsa.sherpa.onnx.voicebridge.MainActivity
import com.k2fsa.sherpa.onnx.voicebridge.R
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import javax.inject.Inject

class NotificationHelper @Inject constructor(
    private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "voicebridge_service"
        const val NOTIFICATION_ID = 1
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VoiceBridge Service",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Active conversation notification"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(state: PipelineState): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val statusText = when (state) {
            PipelineState.IDLE -> "Ready"
            PipelineState.INITIALIZING -> "Initializing models..."
            PipelineState.LISTENING -> "Listening..."
            PipelineState.TRANSCRIBING -> "Transcribing..."
            PipelineState.SENDING -> "Waiting for Claude..."
            PipelineState.SPEAKING -> "Claude is speaking..."
            PipelineState.ENTERTAINING -> "Did you know..."
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("VoiceBridge")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    fun updateNotification(state: PipelineState) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }
}
