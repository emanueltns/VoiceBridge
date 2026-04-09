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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.ActiveCallControls
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.CallDurationTimer
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.ConnectionDot
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.IdleCallButton
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.MessageHistorySheet
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.MeshSphereOrb
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.TranscriptOverlay
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextPrimary
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextSecondary
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextTertiary
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.VBBackground

@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onNavigateToHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMessages by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (audioGranted) {
            viewModel.handleIntent(ConversationIntent.Start)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.handleIntent(ConversationIntent.DismissError)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VBBackground),
    ) {
        if (state.isRunning) {
            ActiveCallLayout(
                state = state,
                onEndCall = { viewModel.handleIntent(ConversationIntent.Stop) },
                onToggleMute = { viewModel.handleIntent(ConversationIntent.ToggleMute) },
                onShowMessages = { showMessages = true },
                onOpenSettings = onOpenSettings,
            )
        } else {
            IdleLayout(
                modelsReady = state.modelsReady,
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 120.dp),
        )

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
    onToggleMute: () -> Unit,
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
        // Top bar: connection dot + status label + settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConnectionDot(connectionState = state.connectionState)
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    targetState = getStateLabel(state.pipelineState),
                    transitionSpec = {
                        (fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                                slideInVertically { it / 2 })
                            .togetherWith(
                                fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) +
                                        slideOutVertically { -it / 2 },
                            )
                    },
                    label = "stateLabel",
                ) { label ->
                    Text(
                        text = label,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                CallDurationTimer(startTimeMs = state.callStartTimeMs)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextTertiary, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.weight(0.08f))

        // The flowing mesh sphere
        MeshSphereOrb(
            pipelineState = state.pipelineState,
            audioAmplitude = state.audioAmplitude,
            size = 300.dp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Live transcript
        TranscriptOverlay(
            text = state.partialTranscript,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.weight(0.12f))

        // 3 ring-style controls: chat, mic/mute, close
        ActiveCallControls(
            isMuted = state.isMuted,
            onToggleMute = onToggleMute,
            onEndCall = onEndCall,
            onShowMessages = onShowMessages,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun IdleLayout(
    modelsReady: Boolean,
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
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onNavigateToHistory) {
                Icon(Icons.Default.History, contentDescription = "History", tint = TextTertiary)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextTertiary)
            }
        }

        Spacer(modifier = Modifier.weight(0.08f))

        // Small app name
        Text(
            text = "VoiceBridge",
            color = TextPrimary.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dormant flowing mesh sphere
        MeshSphereOrb(
            pipelineState = PipelineState.IDLE,
            audioAmplitude = 0f,
            size = 260.dp,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (modelsReady) "Tap to Start" else "Loading models...",
            color = TextSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(0.12f))

        // Green call button (disabled while loading)
        IdleCallButton(
            onClick = onStartCall,
            enabled = modelsReady,
        )

        Spacer(modifier = Modifier.weight(0.12f))
    }
}

private fun getStateLabel(pipeline: PipelineState): String {
    return when (pipeline) {
        PipelineState.IDLE -> ""
        PipelineState.INITIALIZING -> "Loading models..."
        PipelineState.LISTENING -> "Claude is listening.."
        PipelineState.TRANSCRIBING -> "Hearing you..."
        PipelineState.SENDING -> "Claude is thinking..."
        PipelineState.SPEAKING -> "Claude is speaking..."
        PipelineState.ENTERTAINING -> "Fun fact while we wait..."
    }
}
