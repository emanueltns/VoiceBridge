package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.Message
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.CallGreen
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.VBBackground

private val TerminalBg = Color(0xFF0D1117)
private val TerminalHeaderBg = Color(0xFF161B22)
private val DotRed = Color(0xFFFF5F56)
private val DotYellow = Color(0xFFFFBD2E)
private val DotGreen = Color(0xFF27C93F)
private val TerminalDim = Color(0xFF555577)
private val TerminalText = Color(0xFF888899)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageHistorySheet(
    messages: List<Message>,
    onDismiss: () -> Unit,
    onSendText: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TerminalBg,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            // Terminal window header (traffic light dots + title)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(TerminalHeaderBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                // Traffic light dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(DotRed))
                    Box(Modifier.size(10.dp).clip(CircleShape).background(DotYellow))
                    Box(Modifier.size(10.dp).clip(CircleShape).background(DotGreen))
                }
                // Title
                Text(
                    text = "voicebridge ~ terminal",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TerminalDim,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // Terminal body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalBg),
            ) {
                if (messages.isEmpty()) {
                    Text(
                        text = "$ # No messages yet. Start talking!\n$",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TerminalDim,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .padding(vertical = 8.dp),
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(message = message)
                        }
                        // Prompt cursor at end
                        item {
                            Text(
                                text = "$",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = CallGreen,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            // Terminal input prompt
            if (onSendText != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalHeaderBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        color = CallGreen,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Type a command...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = TerminalDim,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TerminalBg,
                            unfocusedContainerColor = TerminalBg,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color(0xFFFF9944),
                            unfocusedTextColor = Color(0xFFFF9944),
                            cursorColor = CallGreen,
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        ),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendText(inputText.trim())
                                inputText = ""
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = CallGreen,
                            contentColor = VBBackground,
                        ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
