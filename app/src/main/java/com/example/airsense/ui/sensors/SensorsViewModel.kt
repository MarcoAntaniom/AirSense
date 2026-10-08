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
                val sensorsList = listOf(
                    SensorDevice("sns_01", "Estación Clima Principal", "AQI + Temp", 95, true, "${metrics.aqi} AQI"),
                    SensorDevice("sns_02", "Sensor Temperatura", "Temp + Humedad", 88, true, "${metrics.temperatureCelsius} °C"),
                    SensorDevice("sns_03", "Sensor Calidad Aire", "CO2 + PM2.5", 90, true, "${metrics.co2Ppm} ppm"),
                    SensorDevice("sns_04", "Sensor Humedad Amb.", "Humedad Relativa", 82, true, "${metrics.humidityPercentage}%")
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
                val mockSensors = listOf(
                    SensorDevice("sns_01", "Estación Clima Principal", "AQI + Temp", 88, false, "${fallbackMetrics.aqi} AQI"),
                    SensorDevice("sns_02", "Sensor Temperatura", "Temp + Humedad", 95, false, "${fallbackMetrics.temperatureCelsius} °C"),
                    SensorDevice("sns_03", "Sensor Calidad Aire", "CO2 + PM2.5", 72, false, "${fallbackMetrics.co2Ppm} ppm"),
                    SensorDevice("sns_04", "Sensor Humedad Amb.", "Humedad Relativa", 80, false, "${fallbackMetrics.humidityPercentage}%")
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

