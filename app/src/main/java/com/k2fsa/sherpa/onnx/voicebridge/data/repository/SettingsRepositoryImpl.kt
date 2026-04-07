package com.k2fsa.sherpa.onnx.voicebridge.data.repository

import com.k2fsa.sherpa.onnx.voicebridge.data.local.preferences.SettingsDataStore
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.ConnectionSettings
import com.k2fsa.sherpa.onnx.voicebridge.domain.repository.SettingsRepository
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
