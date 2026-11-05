package com.example.airsense.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val weatherRepository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadMetrics(
        latitude: Double? = null,
        longitude: Double? = null,
        userPrefs: UserPreferencesManager? = null
    ) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val result = if (latitude != null && longitude != null) {
                weatherRepository.fetchRealtimeMetrics(latitude, longitude)
            } else {
                weatherRepository.fetchRealtimeMetrics()
            }

            result.onSuccess { metrics ->
                userPrefs?.let { prefs ->
                    weatherRepository.saveCachedMetrics(prefs, metrics)
                }
                _uiState.value = HomeUiState.Success(metrics = metrics, isOffline = false)
            }.onFailure {
                val cached = userPrefs?.let { prefs ->
                    weatherRepository.getCachedMetrics(prefs)
                }
                val fallbackMetrics = cached ?: AirQualityMetrics(
                    aqi = 35,
                    aqiStatus = "Excelente",
                    temperature = 22.4f,
                    humidity = 55.0f,
                    gasRaw = 210,
                    co = 2.5f,
                    ch2o = 0.04f,
                    co2 = 450,
                    pm25 = 8.0f,
                    timestamp = System.currentTimeMillis(),
                    lastUpdated = "Modo Local"
                )
                _uiState.value = HomeUiState.Success(
                    metrics = fallbackMetrics,
                    isOffline = true
                )
            }
        }
    }

    fun triggerEmptyState() {
        _uiState.value = HomeUiState.Empty
    }

    fun triggerErrorState() {
        _uiState.value = HomeUiState.Error("No pudimos conectar con la API de clima. Por favor reintenta.")
    }
}
