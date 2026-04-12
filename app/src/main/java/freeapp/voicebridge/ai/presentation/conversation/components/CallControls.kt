package freeapp.voicebridge.ai.presentation.conversation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import freeapp.voicebridge.ai.presentation.theme.CallControlBg
import freeapp.voicebridge.ai.presentation.theme.CallGreen
import freeapp.voicebridge.ai.presentation.theme.CallRed
import freeapp.voicebridge.ai.presentation.theme.TextPrimary
import freeapp.voicebridge.ai.presentation.theme.TextTertiary
import freeapp.voicebridge.ai.presentation.theme.VBBackground

@Composable
fun ActiveCallControls(
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onEndCall: () -> Unit,
    onShowMessages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Chat / Messages button
        OutlinedIconButton(
            onClick = onShowMessages,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, TextTertiary),
            colors = IconButtonDefaults.outlinedIconButtonColors(
                containerColor = CallControlBg,
                contentColor = TextPrimary,
            ),
        ) {
            Icon(Icons.Default.Forum, contentDescription = "Messages", modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(28.dp))

        // Mic / Mute toggle (center, larger)
        OutlinedIconButton(
            onClick = onToggleMute,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            border = BorderStroke(
                2.dp,
                if (isMuted) CallRed else CallGreen,
            ),
            colors = IconButtonDefaults.outlinedIconButtonColors(
                containerColor = CallControlBg,
                contentColor = if (isMuted) CallRed else CallGreen,
            ),
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(modifier = Modifier.width(28.dp))

        // Close / End call
        OutlinedIconButton(
            onClick = onEndCall,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, TextTertiary),
            colors = IconButtonDefaults.outlinedIconButtonColors(
                containerColor = CallControlBg,
                contentColor = TextPrimary,
            ),
        ) {
            Icon(Icons.Default.Close, contentDescription = "End call", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun IdleCallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(80.dp),
        shape = CircleShape,
        enabled = enabled,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = CallGreen,
            contentColor = VBBackground,
            disabledContainerColor = CallGreen.copy(alpha = 0.3f),
            disabledContentColor = VBBackground.copy(alpha = 0.5f),
        ),
    ) {
        Icon(Icons.Default.Call, contentDescription = "Start call", modifier = Modifier.size(32.dp))
    }
}
