package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun CallDurationTimer(
    startTimeMs: Long?,
    modifier: Modifier = Modifier,
) {
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(startTimeMs) {
        if (startTimeMs == null) {
            elapsedSeconds = 0L
            return@LaunchedEffect
        }
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000
            delay(1000)
        }
    }

    if (startTimeMs != null) {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        val formatted = if (minutes >= 60) {
            val hours = minutes / 60
            val mins = minutes % 60
            "%d:%02d:%02d".format(hours, mins, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }

        Text(
            text = formatted,
            color = TextSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
            modifier = modifier,
        )
    }
}
