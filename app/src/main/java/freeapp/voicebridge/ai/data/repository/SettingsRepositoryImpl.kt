package freeapp.voicebridge.ai.data.repository

import freeapp.voicebridge.ai.data.local.preferences.SettingsDataStore
import freeapp.voicebridge.ai.domain.model.ConnectionSettings
import freeapp.voicebridge.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
) : SettingsRepository {

    override val settings: Flow<ConnectionSettings> = dataStore.settings

    override suspend fun getSettings(): ConnectionSettings = dataStore.getSettings()

    override suspend fun saveSettings(settings: ConnectionSettings) {
        dataStore.saveSettings(settings)
    }
}
