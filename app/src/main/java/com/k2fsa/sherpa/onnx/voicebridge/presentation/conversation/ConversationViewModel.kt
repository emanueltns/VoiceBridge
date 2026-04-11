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
    private val pendingTextMessages = mutableListOf<String>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as VoiceBridgeForegroundService.LocalBinder
            service = localBinder.service
            serviceBound = true
            observeService()
            // Flush any text messages that arrived before the service was bound
            for (msg in pendingTextMessages) {
                service?.sendTextMessage(msg)
            }
            pendingTextMessages.clear()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            serviceBound = false
        }
    }

    init {
        observeConnectionState()
        observeModelsReady()
        loadUserName()
    }

    private fun loadUserName() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _state.update {
                it.copy(
                    userName = settings.userName,
                    needsSetup = settings.host.isBlank(),
                )
            }
        }
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
            is ConversationIntent.Start -> startSession()
            is ConversationIntent.Stop -> stopSession()
            is ConversationIntent.DismissError -> _state.update { it.copy(error = null) }
            is ConversationIntent.ToggleMute -> toggleMute()
            is ConversationIntent.SendText -> sendTextMessage(intent.text)
        }
    }

    fun startSession() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            if (settings.host.isBlank()) {
                _state.update { it.copy(needsSetup = true) }
                return@launch
            }

            _state.update { it.copy(needsSetup = false, userName = settings.userName) }

            try {
                vpsRepository.connect(settings.host, settings.port)

                pipelineManager.setAsrEngine(settings.useAndroidAsr)
                pipelineManager.setVoiceId(settings.voiceId)
                pipelineManager.funFactsEnabled = settings.funFactsEnabled

                // Always use one conversation
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

                _state.update { it.copy(isRunning = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to connect: ${e.message}") }
            }
        }
    }

    private fun stopSession() {
        val serviceIntent = Intent(application, VoiceBridgeForegroundService::class.java).apply {
            action = ServiceCommand.ACTION_STOP
        }
        application.startService(serviceIntent)
        _state.update {
            it.copy(
                isRunning = false,
                pipelineState = PipelineState.IDLE,
                audioAmplitude = 0f,
                partialTranscript = "",
            )
        }
    }

    private fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        val svc = service
        if (svc != null) {
            svc.sendTextMessage(text)
        } else {
            // Service not bound yet — queue the message and ensure session starts
            pendingTextMessages.add(text)
            if (!_state.value.isRunning) {
                startSession()
            }
        }
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
        viewModelScope.launch { svc.pipelineState.collect { ps -> _state.update { it.copy(pipelineState = ps) } } }
        viewModelScope.launch { svc.partialResult.collect { p -> _state.update { it.copy(partialTranscript = p) } } }
        viewModelScope.launch { svc.audioAmplitude.collect { a -> _state.update { it.copy(audioAmplitude = a) } } }
        viewModelScope.launch { svc.isMuted.collect { m -> _state.update { it.copy(isMuted = m) } } }
        viewModelScope.launch { svc.streamingResponse.collect { t -> _state.update { it.copy(streamingResponse = t) } } }
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
            try { application.unbindService(serviceConnection) } catch (_: Exception) {}
            serviceBound = false
        }
        super.onCleared()
    }
}
