package com.example.airsense.ui.sensors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.SensorDevice
import com.example.airsense.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SensorsViewModel(
    private val weatherRepository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SensorsUiState>(SensorsUiState.Loading)
    val uiState: StateFlow<SensorsUiState> = _uiState.asStateFlow()

    fun loadSensors(
        latitude: Double? = null,
        longitude: Double? = null,
        userPrefs: UserPreferencesManager? = null
    ) {
        viewModelScope.launch {
            _uiState.value = SensorsUiState.Loading

            val result = if (latitude != null && longitude != null) {
                weatherRepository.fetchRealtimeMetrics(latitude, longitude)
            } else {
                weatherRepository.fetchRealtimeMetrics()
            }

            result.onSuccess { metrics ->
                userPrefs?.let { prefs ->
                    weatherRepository.saveCachedMetrics(prefs, metrics)
                }
                val tempFormatted = String.format(Locale.US, "%.1f °C", metrics.temperature)
                val sensorsList = listOf(
                    SensorDevice("sns_01", "Estación Clima Principal (WeMos D1)", "AQI + Temp", 95, true, "${metrics.aqi} AQI"),
                    SensorDevice("sns_02", "Sensor Temperatura (DHT11)", "Temp + Humedad", 88, true, tempFormatted),
                    SensorDevice("sns_03", "Sensor Calidad Aire (MQ)", "Gas MQ + CO2", 90, true, "${metrics.co2} ppm (Gas: ${metrics.gasRaw})"),
                    SensorDevice("sns_04", "Sensor Humedad Amb.", "Humedad Relativa", 82, true, "${metrics.humidity.toInt()}%")
                )
                _uiState.value = SensorsUiState.Success(
                    metrics = metrics,
                    sensors = sensorsList,
                    isOffline = false
                )
            }.onFailure {
                val cachedMetrics = userPrefs?.let { prefs ->
                    weatherRepository.getCachedMetrics(prefs)
                }
                val fallbackMetrics = cachedMetrics ?: AirQualityMetrics()
                val tempFormatted = String.format(Locale.US, "%.1f °C", fallbackMetrics.temperature)
                val mockSensors = listOf(
                    SensorDevice("sns_01", "Estación Clima Principal (WeMos D1)", "AQI + Temp", 88, false, "${fallbackMetrics.aqi} AQI"),
                    SensorDevice("sns_02", "Sensor Temperatura (DHT11)", "Temp + Humedad", 95, false, tempFormatted),
                    SensorDevice("sns_03", "Sensor Calidad Aire (MQ)", "Gas MQ + CO2", 72, false, "${fallbackMetrics.co2} ppm (Gas: ${fallbackMetrics.gasRaw})"),
                    SensorDevice("sns_04", "Sensor Humedad Amb.", "Humedad Relativa", 80, false, "${fallbackMetrics.humidity.toInt()}%")
                )
                _uiState.value = SensorsUiState.Success(
                    metrics = fallbackMetrics,
                    sensors = mockSensors,
                    isOffline = true
                )
            }
        }
    }

    fun triggerEmptyState() {
        _uiState.value = SensorsUiState.Empty
    }

    fun triggerErrorState() {
        _uiState.value = SensorsUiState.Error("Fallo en la sincronización con los sensores.")
    }
}
