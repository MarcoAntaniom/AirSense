package com.example.airsense.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.airsense.data.models.AirQualityThresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "airsense_user_prefs")

class UserPreferencesManager(private val context: Context) {

    companion object {
        val KEY_IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
        val KEY_CACHED_TELEMETRY = stringPreferencesKey("cached_telemetry")

        val KEY_LAST_NOTIFIED_TIMESTAMP = longPreferencesKey("last_notified_timestamp")
        val KEY_LAST_NOTIFIED_LEVEL = stringPreferencesKey("last_notified_level")

        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"

        // Keys for Thresholds
        val KEY_WARNING_PPM = intPreferencesKey("threshold_warning_ppm")
        val KEY_DANGER_PPM = intPreferencesKey("threshold_danger_ppm")
        val KEY_MAX_TEMP = doublePreferencesKey("threshold_max_temp")
        val KEY_MIN_HUMIDITY = intPreferencesKey("threshold_min_humidity")
        val KEY_MAX_HUMIDITY = intPreferencesKey("threshold_max_humidity")

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

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: run {
            if (preferences.contains(KEY_DARK_MODE)) {
                if (preferences[KEY_DARK_MODE] == true) THEME_DARK else THEME_LIGHT
            } else {
                THEME_SYSTEM
            }
        }
    }

    val isDarkModeFlow: Flow<Boolean> = themeModeFlow.map { mode ->
        mode == THEME_DARK
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

    val lastNotifiedTimestampFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_NOTIFIED_TIMESTAMP] ?: 0L
    }

    val lastNotifiedLevelFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_NOTIFIED_LEVEL] ?: "GOOD"
    }

    val thresholdsFlow: Flow<AirQualityThresholds> = context.dataStore.data.map { preferences ->
        AirQualityThresholds(
            warningPpm = preferences[KEY_WARNING_PPM] ?: 800,
            dangerPpm = preferences[KEY_DANGER_PPM] ?: 1200,
            maxTemperatureCelsius = preferences[KEY_MAX_TEMP] ?: 35.0,
            minHumidityPercentage = preferences[KEY_MIN_HUMIDITY] ?: 30,
            maxHumidityPercentage = preferences[KEY_MAX_HUMIDITY] ?: 70
        )
    }

    suspend fun setFirstRunCompleted() {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_FIRST_RUN] = false
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
            preferences[KEY_DARK_MODE] = (mode == THEME_DARK)
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        setThemeMode(if (enabled) THEME_DARK else THEME_LIGHT)
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

    suspend fun saveLastNotificationState(timestamp: Long, levelName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_NOTIFIED_TIMESTAMP] = timestamp
            preferences[KEY_LAST_NOTIFIED_LEVEL] = levelName
        }
    }

    suspend fun saveThresholds(thresholds: AirQualityThresholds): Boolean {
        if (!thresholds.isValid()) return false
        context.dataStore.edit { preferences ->
            preferences[KEY_WARNING_PPM] = thresholds.warningPpm
            preferences[KEY_DANGER_PPM] = thresholds.dangerPpm
            preferences[KEY_MAX_TEMP] = thresholds.maxTemperatureCelsius
            preferences[KEY_MIN_HUMIDITY] = thresholds.minHumidityPercentage
            preferences[KEY_MAX_HUMIDITY] = thresholds.maxHumidityPercentage
        }
        return true
    }
}
