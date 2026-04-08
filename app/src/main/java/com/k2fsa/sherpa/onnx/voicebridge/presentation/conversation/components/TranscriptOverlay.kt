package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TranscriptText

@Composable
fun TranscriptOverlay(
    text: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = text.isNotBlank(),
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500)),
        modifier = modifier,
    ) {
        Text(
            text = text,
            color = TranscriptText,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
