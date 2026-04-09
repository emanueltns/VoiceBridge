package com.k2fsa.sherpa.onnx.voicebridge.presentation.conversation

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.PipelineState
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.ConversationRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.SettingsRepository
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.VpsRepository
import com.k2fsa.sherpa.onnx.voicebridge.service.AudioPipelineManager
import com.k2fsa.sherpa.onnx.voicebridge.service.ServiceCommand
import com.k2fsa.sherpa.onnx.voicebridge.service.VoiceBridgeForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val application: Application,
    private val conversationRepository: ConversationRepository,
    private val settingsRepository: SettingsRepository,
    private val vpsRepository: VpsRepository,
    private val pipelineManager: AudioPipelineManager,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ConversationUiState())
    val state: StateFlow<ConversationUiState> = _state.asStateFlow()

    private var serviceBound = false
    private var service: VoiceBridgeForegroundService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as VoiceBridgeForegroundService.LocalBinder
            service = localBinder.service
            serviceBound = true
            observeService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            serviceBound = false
        }
    }

    init {
        observeConnectionState()
        observeModelsReady()
    }

    private fun observeModelsReady() {
        viewModelScope.launch {
            pipelineManager.isReady.collect { ready ->
                _state.update { it.copy(modelsReady = ready) }
            }
        }
    }

    fun handleIntent(intent: ConversationIntent) {
        when (intent) {
            is ConversationIntent.Start -> startConversation()
            is ConversationIntent.Stop -> stopConversation()
            is ConversationIntent.NewConversation -> newConversation()
            is ConversationIntent.DismissError -> _state.update { it.copy(error = null) }
            is ConversationIntent.OpenHistory -> { /* handled by navigation */ }
            is ConversationIntent.OpenSettings -> { /* handled by navigation */ }
            is ConversationIntent.ToggleMute -> toggleMute()
            is ConversationIntent.SendText -> sendTextMessage(intent.text)
        }
    }

    private fun startConversation() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            if (settings.host.isBlank()) {
                _state.update { it.copy(error = "Please configure VPS host in settings") }
                return@launch
            }

            try {
                vpsRepository.connect(settings.host, settings.port)

                // Apply ASR + voice settings BEFORE starting the pipeline
                pipelineManager.setAsrEngine(settings.useAndroidAsr)
                pipelineManager.setVoiceId(settings.voiceId)

                val conversation = conversationRepository.getLastActiveConversationId()?.let {
                    conversationRepository.getConversation(it)
                } ?: conversationRepository.createConversation()

                val serviceIntent = Intent(application, VoiceBridgeForegroundService::class.java).apply {
                    action = ServiceCommand.ACTION_START
                    putExtra("conversation_id", conversation.id)
                }
                ContextCompat.startForegroundService(application, serviceIntent)

                bindToService()
                observeMessages(conversation.id)

                _state.update {
                    it.copy(
                        isRunning = true,
                        callStartTimeMs = System.currentTimeMillis(),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to start: ${e.message}") }
            }
        }
    }

    private fun stopConversation() {
        val serviceIntent = Intent(application, VoiceBridgeForegroundService::class.java).apply {
            action = ServiceCommand.ACTION_STOP
        }
        application.startService(serviceIntent)
        _state.update {
            it.copy(
                isRunning = false,
                pipelineState = PipelineState.IDLE,
                callStartTimeMs = null,
                audioAmplitude = 0f,
                partialTranscript = "",
            )
        }
    }

    private fun newConversation() {
        viewModelScope.launch {
            val conversation = conversationRepository.createConversation()
            observeMessages(conversation.id)

            // Restart service with new conversation
            if (_state.value.isRunning) {
                val serviceIntent = Intent(application, VoiceBridgeForegroundService::class.java).apply {
                    action = ServiceCommand.ACTION_START
                    putExtra("conversation_id", conversation.id)
                }
                ContextCompat.startForegroundService(application, serviceIntent)
            }
        }
    }

    private fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        service?.sendTextMessage(text)
    }

    private fun toggleMute() {
        val svc = service ?: return
        val newMuted = !_state.value.isMuted
        svc.setMuted(newMuted)
        _state.update { it.copy(isMuted = newMuted) }
    }

    private fun bindToService() {
        if (!serviceBound) {
            val intent = Intent(application, VoiceBridgeForegroundService::class.java)
            application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun observeService() {
        val svc = service ?: return
        viewModelScope.launch {
            svc.pipelineState.collect { pipelineState ->
                _state.update { it.copy(pipelineState = pipelineState) }
            }
        }
        viewModelScope.launch {
            svc.currentConversationId.collect { convId ->
                if (convId != null) {
                    observeMessages(convId)
                }
            }
        }
        viewModelScope.launch {
            svc.partialResult.collect { partial ->
                _state.update { it.copy(partialTranscript = partial) }
            }
        }
        viewModelScope.launch {
            svc.audioAmplitude.collect { amplitude ->
                _state.update { it.copy(audioAmplitude = amplitude) }
            }
        }
        viewModelScope.launch {
            svc.isMuted.collect { muted ->
                _state.update { it.copy(isMuted = muted) }
            }
        }
        viewModelScope.launch {
            svc.streamingResponse.collect { text ->
                _state.update { it.copy(streamingResponse = text) }
            }
        }
    }

    private fun observeMessages(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.getMessages(conversationId).collect { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            vpsRepository.connectionState.collect { connectionState ->
                _state.update { it.copy(connectionState = connectionState) }
            }
        }
    }

    override fun onCleared() {
        if (serviceBound) {
            try {
                application.unbindService(serviceConnection)
            } catch (_: Exception) {}
            serviceBound = false
        }
        super.onCleared()
    }
}
