package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.MessageBubble
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.CallGreen
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextPrimary
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextTertiary

private val TerminalBg = Color(0xFF0D1117)
private val TerminalHeaderBg = Color(0xFF161B22)
private val DotRed = Color(0xFFFF5F56)
private val DotYellow = Color(0xFFFFBD2E)
private val DotGreen = Color(0xFF27C93F)
private val TerminalDim = Color(0xFF555577)

@Composable
fun ConversationDetailScreen(
    conversationId: String,
    viewModel: ConversationDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .statusBarsPadding(),
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                )
            }
            Text(
                text = state.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${state.messages.size} msgs",
                color = TextTertiary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 16.dp),
            )
        }

        // Terminal window header (traffic lights)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(TerminalHeaderBg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(DotRed))
                Box(Modifier.size(10.dp).clip(CircleShape).background(DotYellow))
                Box(Modifier.size(10.dp).clip(CircleShape).background(DotGreen))
            }
            Text(
                text = "voicebridge ~ history",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TerminalDim,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Terminal body with messages
        if (state.messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
                    .background(TerminalBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$ # Empty conversation",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = TerminalDim,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
                    .background(TerminalBg)
                    .padding(vertical = 8.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
                item {
                    Text(
                        text = "$ # End of conversation",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TerminalDim,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
