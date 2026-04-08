package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.CallControlBg
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.CallGreen
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.CallRed
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextPrimary

@Composable
fun ActiveCallControls(
    onNewConversation: () -> Unit,
    onEndCall: () -> Unit,
    onShowMessages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // New conversation
        FilledIconButton(
            onClick = onNewConversation,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = CallControlBg,
                contentColor = TextPrimary,
            ),
        ) {
            Icon(Icons.Default.Add, contentDescription = "New conversation", modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(32.dp))

        // End call (larger)
        FilledIconButton(
            onClick = onEndCall,
            modifier = Modifier.size(68.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = CallRed,
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Default.CallEnd, contentDescription = "End call", modifier = Modifier.size(30.dp))
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Messages
        FilledIconButton(
            onClick = onShowMessages,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = CallControlBg,
                contentColor = TextPrimary,
            ),
        ) {
            Icon(Icons.Default.Forum, contentDescription = "Messages", modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun IdleCallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .width(220.dp)
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CallGreen,
            contentColor = Color.White,
        ),
    ) {
        Icon(
            Icons.Default.Call,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text("Call Claude", fontSize = 17.sp)
    }
}
