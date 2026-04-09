package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Message
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.MessageRole

// Terminal colors
private val TerminalPrompt = Color(0xFFFF9944)   // Orange for $ prompt
private val TerminalUserText = Color(0xFF999999)  // Dim gray for user input
private val TerminalResponse = Color(0xFFC0C0E0)  // Light gray for Claude
private val TerminalHighlight = Color(0xFF00FF41) // Green for key output
private val TerminalError = Color(0xFFFF4444)     // Red for errors

/**
 * Terminal-style message display.
 * User: $ "message"
 * Claude: plain text
 * System: red text
 */
@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
    ) {
        when (message.role) {
            MessageRole.USER -> {
                Text(
                    text = "$ \"${message.text}\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = TerminalPrompt,
                    lineHeight = 18.sp,
                )
            }
            MessageRole.ASSISTANT -> {
                // Split into lines, highlight last line green (like terminal "result")
                val lines = message.text.lines()
                for ((index, line) in lines.withIndex()) {
                    if (line.isBlank()) continue
                    val isLastLine = index == lines.lastIndex
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = if (isLastLine && lines.size > 1) TerminalHighlight else TerminalResponse,
                        lineHeight = 18.sp,
                    )
                }
            }
            MessageRole.SYSTEM -> {
                Text(
                    text = "! ${message.text}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = TerminalError,
                    lineHeight = 18.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
