package freeapp.voicebridge.ai.presentation.settings

import android.content.res.AssetManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import freeapp.voicebridge.ai.domain.model.ConnectionSettings
import freeapp.voicebridge.ai.domain.repository.SettingsRepository
import freeapp.voicebridge.ai.service.AudioPipelineManager
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
    val useAndroidAsr: Boolean = true,
    val funFactsEnabled: Boolean = true,
    val userName: String = "",
    val saved: Boolean = false,
    val offlineModelAvailable: Boolean = false,
    val showDownloadDialog: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val pipelineManager: AudioPipelineManager,
    private val assetManager: AssetManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        checkOfflineModelAvailable()
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

    private fun checkOfflineModelAvailable() {
        val available = try {
            val files = assetManager.list("sherpa-onnx-nemotron-en")
            files != null && files.contains("encoder.int8.onnx")
        } catch (_: Exception) { false }
        _state.update { it.copy(offlineModelAvailable = available) }
    }

    fun onHostChanged(host: String) { _state.update { it.copy(host = host, saved = false) } }
    fun onPortChanged(port: String) { _state.update { it.copy(port = port, saved = false) } }
    fun onVoiceIdChanged(voiceId: Int) { _state.update { it.copy(voiceId = voiceId, saved = false) } }
    fun onFunFactsChanged(enabled: Boolean) { _state.update { it.copy(funFactsEnabled = enabled, saved = false) } }
    fun onAsrEngineChanged(useAndroid: Boolean) {
        // If switching to offline and model not available, show download dialog
        if (!useAndroid && !_state.value.offlineModelAvailable) {
            _state.update { it.copy(showDownloadDialog = true) }
            return
        }
        _state.update { it.copy(useAndroidAsr = useAndroid, saved = false) }
    }

    fun dismissDownloadDialog() {
        _state.update { it.copy(showDownloadDialog = false) }
    }
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
