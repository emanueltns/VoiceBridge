package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.OrbEntertaining
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.OrbIdle
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.OrbInitializing
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.OrbListening
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.OrbSending
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.OrbSpeaking
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.OrbTranscribing

@Composable
fun ClaudeOrb(
    pipelineState: PipelineState,
    audioAmplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
) {
    val targetColor = when (pipelineState) {
        PipelineState.IDLE -> OrbIdle
        PipelineState.INITIALIZING -> OrbInitializing
        PipelineState.LISTENING -> OrbListening
        PipelineState.TRANSCRIBING -> OrbTranscribing
        PipelineState.SENDING -> OrbSending
        PipelineState.SPEAKING -> OrbSpeaking
        PipelineState.ENTERTAINING -> OrbEntertaining
    }

    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(800),
        label = "orbColor",
    )

    // Amplitude-reactive scale for listening/transcribing
    val amplitudeScale by animateFloatAsState(
        targetValue = when (pipelineState) {
            PipelineState.LISTENING, PipelineState.TRANSCRIBING ->
                0.95f + audioAmplitude * 0.2f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.6f),
        label = "ampScale",
    )

    // State-specific pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")

    val breathScale by infiniteTransition.animateFloat(
        initialValue = when (pipelineState) {
            PipelineState.IDLE -> 0.97f
            PipelineState.LISTENING -> 0.92f
            PipelineState.SENDING -> 0.88f
            PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 0.90f
            else -> 0.95f
        },
        targetValue = when (pipelineState) {
            PipelineState.IDLE -> 1.03f
            PipelineState.LISTENING -> 1.08f
            PipelineState.SENDING -> 1.12f
            PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 1.10f
            else -> 1.05f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (pipelineState) {
                    PipelineState.IDLE -> 8000
                    PipelineState.LISTENING -> 1500
                    PipelineState.SENDING -> 2000
                    PipelineState.SPEAKING, PipelineState.ENTERTAINING -> 600
                    PipelineState.INITIALIZING -> 1000
                    else -> 1200
                },
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathScale",
    )

    // Rotation for initializing state
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    // Expanding ring for speaking state
    val ringExpand by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringExpand",
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringAlpha",
    )

    val combinedScale = breathScale * amplitudeScale

    Canvas(modifier = modifier.size(size)) {
        val center = this.center
        val baseRadius = this.size.minDimension / 2f * 0.42f

        // Layer 1: Outer glow
        drawCircle(
            color = color.copy(alpha = 0.08f),
            radius = baseRadius * combinedScale * 1.6f,
            center = center,
        )

        // Expanding ring (speaking/entertaining only)
        if (pipelineState == PipelineState.SPEAKING || pipelineState == PipelineState.ENTERTAINING) {
            drawCircle(
                color = color.copy(alpha = ringAlpha),
                radius = baseRadius * ringExpand,
                center = center,
            )
        }

        // Layer 2: Mid ring
        drawCircle(
            color = color.copy(alpha = 0.2f),
            radius = baseRadius * combinedScale * 1.2f,
            center = center,
        )

        // Layer 3: Core orb with radial gradient
        val coreRadius = baseRadius * combinedScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.9f),
                    color,
                    color.copy(alpha = 0.7f),
                ),
                center = if (pipelineState == PipelineState.INITIALIZING) {
                    // Rotating highlight during initialization
                    val rad = Math.toRadians(rotation.toDouble())
                    Offset(
                        center.x + (coreRadius * 0.3f * kotlin.math.cos(rad)).toFloat(),
                        center.y + (coreRadius * 0.3f * kotlin.math.sin(rad)).toFloat(),
                    )
                } else {
                    Offset(center.x - coreRadius * 0.2f, center.y - coreRadius * 0.2f)
                },
                radius = coreRadius * 1.5f,
            ),
            radius = coreRadius,
            center = center,
        )

        // Layer 4: Inner glass highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0f),
                ),
                center = Offset(center.x - coreRadius * 0.25f, center.y - coreRadius * 0.3f),
                radius = coreRadius * 0.6f,
            ),
            radius = coreRadius * 0.5f,
            center = Offset(center.x - coreRadius * 0.15f, center.y - coreRadius * 0.2f),
        )
    }
}
