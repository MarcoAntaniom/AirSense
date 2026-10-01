package com.example.airsense.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "airsense_user_prefs")

class UserPreferencesManager(private val context: Context) {

    companion object {
        val KEY_IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
        val KEY_CACHED_TELEMETRY = stringPreferencesKey("cached_telemetry")

        fun formatTemperature(celsius: Double, useFahrenheit: Boolean): String {
            return if (useFahrenheit) {
                val fahrenheit = (celsius * 1.8) + 32
                String.format(Locale.US, "%.1f °F", fahrenheit)
            } else {
                String.format(Locale.US, "%.1f °C", celsius)
            }
        }
    }

    val isFirstRunFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_FIRST_RUN] ?: true
    }

    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_MODE] ?: false
    }

    val isNotificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val useFahrenheitFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_USE_FAHRENHEIT] ?: false
    }

    val cachedTelemetryFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CACHED_TELEMETRY] ?: ""
    }

    suspend fun setFirstRunCompleted() {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_FIRST_RUN] = false
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setUseFahrenheit(useFahrenheit: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USE_FAHRENHEIT] = useFahrenheit
        }
    }

    suspend fun saveCachedTelemetry(json: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CACHED_TELEMETRY] = json
        }
    }
}