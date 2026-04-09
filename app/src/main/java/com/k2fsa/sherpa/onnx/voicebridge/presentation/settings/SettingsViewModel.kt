package com.k2fsa.sherpa.onnx.voicebridge.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.ConnectionSettings
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.SettingsRepository
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
    val saved: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
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
                    )
                }
            }
        }
    }

    fun onHostChanged(host: String) {
        _state.update { it.copy(host = host, saved = false) }
    }

    fun onPortChanged(port: String) {
        _state.update { it.copy(port = port, saved = false) }
    }

    fun onVoiceIdChanged(voiceId: Int) {
        _state.update { it.copy(voiceId = voiceId, saved = false) }
    }

    fun onFunFactsChanged(enabled: Boolean) {
        _state.update { it.copy(funFactsEnabled = enabled, saved = false) }
    }

    fun onAsrEngineChanged(useAndroid: Boolean) {
        _state.update { it.copy(useAndroidAsr = useAndroid, saved = false) }
    }

    fun save() {
        viewModelScope.launch {
            val port = _state.value.port.toIntOrNull() ?: 9999
            settingsRepository.saveSettings(
                ConnectionSettings(_state.value.host, port, _state.value.voiceId, _state.value.useAndroidAsr, _state.value.funFactsEnabled),
            )
            _state.update { it.copy(saved = true) }
        }
    }
}
