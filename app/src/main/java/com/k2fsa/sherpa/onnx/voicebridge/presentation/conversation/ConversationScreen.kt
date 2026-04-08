package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.ConnectionState
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.ActiveCallControls
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.CallDurationTimer
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.ClaudeOrb
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.ConnectionDot
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.IdleCallButton
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.MessageHistorySheet
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.TranscriptOverlay
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.CallBackgroundBottom
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.CallBackgroundTop
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextPrimary
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextSecondary
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextTertiary

private val callGradient = Brush.verticalGradient(
    colors = listOf(CallBackgroundTop, CallBackgroundBottom),
)

@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onNavigateToHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMessages by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (audioGranted) {
            viewModel.handleIntent(ConversationIntent.Start)
        }
    }

    // Show errors
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.handleIntent(ConversationIntent.DismissError)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(callGradient),
    ) {
        if (state.isRunning) {
            ActiveCallLayout(
                state = state,
                onEndCall = { viewModel.handleIntent(ConversationIntent.Stop) },
                onNewConversation = { viewModel.handleIntent(ConversationIntent.NewConversation) },
                onShowMessages = { showMessages = true },
                onOpenSettings = onOpenSettings,
            )
        } else {
            IdleLayout(
                onStartCall = {
                    val permissions = buildList {
                        add(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                },
                onNavigateToHistory = onNavigateToHistory,
                onOpenSettings = onOpenSettings,
            )
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 100.dp),
        )

        // Message history bottom sheet
        if (showMessages) {
            MessageHistorySheet(
                messages = state.messages,
                onDismiss = { showMessages = false },
            )
        }
    }
}

@Composable
private fun ActiveCallLayout(
    state: ConversationUiState,
    onEndCall: () -> Unit,
    onNewConversation: () -> Unit,
    onShowMessages: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar: connection + timer + settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConnectionDot(connectionState = state.connectionState)
            Spacer(modifier = Modifier.weight(1f))
            CallDurationTimer(startTimeMs = state.callStartTimeMs)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextTertiary,
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.15f))

        // Claude Orb
        ClaudeOrb(
            pipelineState = state.pipelineState,
            audioAmplitude = state.audioAmplitude,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // State label with animated transitions
        AnimatedContent(
            targetState = getStateLabel(state.pipelineState),
            transitionSpec = {
                (fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                        slideInVertically { it / 2 })
                    .togetherWith(
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) +
                                slideOutVertically { -it / 2 }
                    )
            },
            label = "stateLabel",
        ) { label ->
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live transcript
        TranscriptOverlay(
            text = state.partialTranscript,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(0.25f))

        // Call controls
        ActiveCallControls(
            onNewConversation = onNewConversation,
            onEndCall = onEndCall,
            onShowMessages = onShowMessages,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun IdleLayout(
    onStartCall: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar: history + settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onNavigateToHistory) {
                Icon(Icons.Default.History, contentDescription = "History", tint = TextTertiary)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextTertiary)
            }
        }

        Spacer(modifier = Modifier.weight(0.25f))

        // Dormant orb
        ClaudeOrb(
            pipelineState = PipelineState.IDLE,
            audioAmplitude = 0f,
            size = 180.dp,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "VoiceBridge",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(40.dp))

        IdleCallButton(onClick = onStartCall)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tap to start a voice conversation",
            color = TextTertiary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(0.35f))
    }
}

private fun getStateLabel(pipeline: PipelineState): String {
    return when (pipeline) {
        PipelineState.IDLE -> ""
        PipelineState.INITIALIZING -> "Loading models..."
        PipelineState.LISTENING -> "Listening..."
        PipelineState.TRANSCRIBING -> "Hearing you..."
        PipelineState.SENDING -> "Claude is thinking..."
        PipelineState.SPEAKING -> "Claude is speaking..."
        PipelineState.ENTERTAINING -> "Fun fact while we wait..."
    }
}
