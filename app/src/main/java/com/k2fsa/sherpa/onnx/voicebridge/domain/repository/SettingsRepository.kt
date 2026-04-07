package com.k2fsa.sherpa.onnx.voicebridge.domain.repository

import com.k2fsa.sherpa.onnx.voicebridge.domain.model.ConnectionSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<ConnectionSettings>
    suspend fun getSettings(): ConnectionSettings
    suspend fun saveSettings(settings: ConnectionSettings)
}
