package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.MessageBubble
import com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation.components.MeshSphereOrb
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.CallGreen
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.CallRed
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextPrimary
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.TextTertiary
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.VBBackground
import com.k2fsa.sherpa.onnx.voicebridge.presentation.theme.VBSurface

private val TerminalBg = Color(0xFF0D1117)
private val TerminalHeaderBg = Color(0xFF161B22)
private val DotRed = Color(0xFFFF5F56)
private val DotYellow = Color(0xFFFFBD2E)
private val DotGreen = Color(0xFF27C93F)
private val TerminalDim = Color(0xFF555577)

@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onNavigateToHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

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

    // Auto-scroll on new messages
    LaunchedEffect(state.messages.size, state.streamingResponse) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // ── Loading Screen ──
    if (!state.modelsReady) {
        LoadingScreen()
        return
    }

    // ── Main Screen ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalHeaderBg)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // History
            IconButton(onClick = onNavigateToHistory, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.History, contentDescription = "History", tint = TextTertiary, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Small orb indicator (shows state color)
            if (state.isRunning) {
                MeshSphereOrb(
                    pipelineState = state.pipelineState,
                    audioAmplitude = state.audioAmplitude,
                    size = 36.dp,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = getStateLabel(state.pipelineState),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                // Traffic light dots when idle
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(DotRed))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(DotYellow))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(DotGreen))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "voicebridge",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TerminalDim,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Mic toggle (when running)
            if (state.isRunning) {
                IconButton(
                    onClick = { viewModel.handleIntent(ConversationIntent.ToggleMute) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (state.isMuted) "Unmute" else "Mute",
                        tint = if (state.isMuted) CallRed else CallGreen,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // New conversation (always starts fresh terminal)
            IconButton(
                onClick = {
                    if (state.isRunning) {
                        // Already running — create new conversation in same session
                        viewModel.handleIntent(ConversationIntent.NewConversation)
                    } else {
                        // Not running — start call with new conversation
                        val permissions = buildList {
                            add(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "New conversation", tint = CallGreen, modifier = Modifier.size(20.dp))
            }

            // Settings
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
        }

        // ── Terminal Body (messages) ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(TerminalBg),
        ) {
            if (state.messages.isEmpty() && !state.isRunning) {
                // Empty state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MeshSphereOrb(
                        pipelineState = PipelineState.IDLE,
                        audioAmplitude = 0f,
                        size = 160.dp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "$ # Tap + to start a conversation",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TerminalDim,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }

                    // Show streaming response as it arrives
                    if (state.streamingResponse.isNotBlank()) {
                        item(key = "streaming") {
                            Text(
                                text = state.streamingResponse,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = CallGreen.copy(alpha = 0.6f),
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp),
                            )
                        }
                    }

                    // Show partial transcript while user speaks
                    if (state.partialTranscript.isNotBlank()) {
                        item(key = "partial") {
                            Text(
                                text = "$ \"${state.partialTranscript}\" ...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFFFF9944).copy(alpha = 0.5f),
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp),
                            )
                        }
                    }

                    // Prompt cursor
                    item(key = "cursor") {
                        Text(
                            text = "$",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = if (state.isRunning) CallGreen else TerminalDim,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        // ── Bottom Input Bar ──
        if (state.isRunning) {
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
                        Text("Type or speak...", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = TerminalDim)
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
                Spacer(modifier = Modifier.width(8.dp))

                // Send or Stop
                if (inputText.isNotBlank()) {
                    FilledIconButton(
                        onClick = {
                            viewModel.handleIntent(ConversationIntent.SendText(inputText.trim()))
                            inputText = ""
                        },
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = CallGreen,
                            contentColor = VBBackground,
                        ),
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                    }
                } else {
                    OutlinedIconButton(
                        onClick = { viewModel.handleIntent(ConversationIntent.Stop) },
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CallRed.copy(alpha = 0.5f)),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            contentColor = CallRed,
                        ),
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "End", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

/**
 * Loading screen with mesh orb + Windows 95 style green progress bar.
 * Blocks appear one at a time like the classic Win95/97 loading animation.
 */
@Composable
private fun LoadingScreen() {
    val totalBlocks = 18
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    // Animate from 0 to totalBlocks, each block appears one at a time
    val blockProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = totalBlocks.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "blocks",
    )

    val visibleBlocks = blockProgress.toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VBBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MeshSphereOrb(
            pipelineState = PipelineState.INITIALIZING,
            audioAmplitude = 0f,
            size = 200.dp,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Loading...",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Win95 progress bar: sunken border + green blocks
        Box(
            modifier = Modifier
                .width(240.dp)
                .height(24.dp)
                .background(Color(0xFF333344), RoundedCornerShape(1.dp))
                .padding(1.dp)
                .background(Color(0xFF0A0A14), RoundedCornerShape(1.dp))
                .padding(3.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(totalBlocks) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .background(
                                if (index < visibleBlocks) CallGreen else Color.Transparent,
                                RoundedCornerShape(1.dp),
                            ),
                    )
                }
            }
        }
    }
}

private fun getStateLabel(pipeline: PipelineState): String {
    return when (pipeline) {
        PipelineState.IDLE -> ""
        PipelineState.INITIALIZING -> "loading..."
        PipelineState.LISTENING -> "listening"
        PipelineState.TRANSCRIBING -> "hearing..."
        PipelineState.SENDING -> "thinking..."
        PipelineState.SPEAKING -> "speaking"
        PipelineState.ENTERTAINING -> "fun fact"
    }
}
