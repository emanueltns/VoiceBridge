package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.ConnectionState
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.DotConnected
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.DotConnecting
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.DotDisconnected
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.DotError
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextTertiary

@Composable
fun ConnectionDot(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val color = when (connectionState) {
        is ConnectionState.Connected -> DotConnected
        is ConnectionState.Connecting -> DotConnecting
        is ConnectionState.Disconnected -> DotDisconnected
        is ConnectionState.Error -> DotError
    }

    val label = when (connectionState) {
        is ConnectionState.Connected -> "Connected"
        is ConnectionState.Connecting -> "Connecting..."
        is ConnectionState.Disconnected -> "Offline"
        is ConnectionState.Error -> "Reconnecting..."
    }

    val shouldBlink = connectionState is ConnectionState.Connecting ||
            connectionState is ConnectionState.Error

    val transition = rememberInfiniteTransition(label = "dotBlink")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (shouldBlink) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotAlpha",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color.copy(alpha = alpha))
        }
        Text(
            text = label,
            color = TextTertiary,
            fontSize = 12.sp,
        )
    }
}
