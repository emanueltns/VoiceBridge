package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.MessageBubble
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextPrimary
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextSecondary
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextTertiary
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.VBBackground

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
            .background(VBBackground)
            .statusBarsPadding(),
    ) {
        // Header
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${state.messages.size} messages",
                color = TextTertiary,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 16.dp),
            )
        }

        if (state.messages.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "No messages in this conversation",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
