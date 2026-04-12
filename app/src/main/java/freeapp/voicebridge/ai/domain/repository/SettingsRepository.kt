package freeapp.voicebridge.ai.domain.repository

import freeapp.voicebridge.ai.domain.model.ConnectionSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<ConnectionSettings>
    suspend fun getSettings(): ConnectionSettings
    suspend fun saveSettings(settings: ConnectionSettings)
}
