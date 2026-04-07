package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.StateEntertaining
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.StateIdle
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.StateListening
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.StateSending
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.StateSpeaking
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.StateTranscribing

@Composable
fun StatusIndicator(
    state: PipelineState,
    modifier: Modifier = Modifier,
) {
    val targetColor = when (state) {
        PipelineState.IDLE -> StateIdle
        PipelineState.INITIALIZING -> StateIdle
        PipelineState.LISTENING -> StateListening
        PipelineState.TRANSCRIBING -> StateTranscribing
        PipelineState.SENDING -> StateSending
        PipelineState.SPEAKING -> StateSpeaking
        PipelineState.ENTERTAINING -> StateEntertaining
    }

    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(500),
        label = "statusColor",
    )

    val shouldPulse = state != PipelineState.IDLE

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    PipelineState.LISTENING -> 1200
                    PipelineState.SPEAKING -> 600
                    PipelineState.SENDING -> 1800
                    else -> 1000
                },
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val scale = if (shouldPulse) pulseScale else 1f

    Canvas(modifier = modifier.size(140.dp)) {
        val radius = size.minDimension / 2f * scale

        // Outer glow
        drawCircle(
            color = color.copy(alpha = 0.15f),
            radius = radius * 1.3f,
        )

        // Middle ring
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = radius * 1.1f,
        )

        // Core circle
        drawCircle(
            color = color,
            radius = radius * 0.7f,
        )
    }
}
