package com.k2fsa.sherpa.onnx.voicebridge.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.k2fsa.sherpa.onnx.voicebridge.domain.model.ConnectionSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context,
) {
    private val hostKey = stringPreferencesKey("vps_host")
    private val portKey = intPreferencesKey("vps_port")

    val settings: Flow<ConnectionSettings> = context.dataStore.data.map { prefs ->
        ConnectionSettings(
            host = prefs[hostKey] ?: "",
            port = prefs[portKey] ?: 9999,
        )
    }

    suspend fun getSettings(): ConnectionSettings {
        return settings.first()
    }

    suspend fun saveSettings(settings: ConnectionSettings) {
        context.dataStore.edit { prefs ->
            prefs[hostKey] = settings.host
            prefs[portKey] = settings.port
        }
    }
}
