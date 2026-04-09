package com.k2fsa.sherpa.onnx.voicebridge.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.ConnectionSettings
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.SettingsRepository
import com.k2fsa.sherpa.onnx.voicebridge.service.AudioPipelineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val host: String = "",
    val port: String = "9999",
    val voiceId: Int = 0,
    val useAndroidAsr: Boolean = false,
    val funFactsEnabled: Boolean = true,
    val userName: String = "",
    val saved: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val pipelineManager: AudioPipelineManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update {
                    it.copy(
                        host = settings.host,
                        port = settings.port.toString(),
                        voiceId = settings.voiceId,
                        useAndroidAsr = settings.useAndroidAsr,
                        funFactsEnabled = settings.funFactsEnabled,
                        userName = settings.userName,
                    )
                }
            }
        }
    }

    fun onHostChanged(host: String) { _state.update { it.copy(host = host, saved = false) } }
    fun onPortChanged(port: String) { _state.update { it.copy(port = port, saved = false) } }
    fun onVoiceIdChanged(voiceId: Int) { _state.update { it.copy(voiceId = voiceId, saved = false) } }
    fun onFunFactsChanged(enabled: Boolean) { _state.update { it.copy(funFactsEnabled = enabled, saved = false) } }
    fun onAsrEngineChanged(useAndroid: Boolean) { _state.update { it.copy(useAndroidAsr = useAndroid, saved = false) } }
    fun onUserNameChanged(name: String) { _state.update { it.copy(userName = name, saved = false) } }

    fun save() {
        viewModelScope.launch {
            val port = _state.value.port.toIntOrNull() ?: 9999
            val s = _state.value
            settingsRepository.saveSettings(
                ConnectionSettings(s.host, port, s.voiceId, s.useAndroidAsr, s.funFactsEnabled, s.userName),
            )

            // Apply to running pipeline immediately
            pipelineManager.funFactsEnabled = s.funFactsEnabled
            pipelineManager.setVoiceId(s.voiceId)
            pipelineManager.setAsrEngine(s.useAndroidAsr)

            _state.update { it.copy(saved = true) }
        }
    }
}
