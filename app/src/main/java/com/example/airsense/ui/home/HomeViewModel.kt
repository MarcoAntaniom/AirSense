package com.example.airsense.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airsense.data.models.AirQualityMetrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
    }

    fun loadMetrics() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            delay(800) // Simular latencia de red / sensor
            try {
                val metrics = AirQualityMetrics(
                    aqi = 28,
                    aqiStatus = "Excelente",
                    temperatureCelsius = 22.5,
                    humidityPercentage = 54,
                    co2Ppm = 420,
                    pm25Ug = 8,
                    lastUpdated = "Hace 1 min"
                )
                _uiState.value = HomeUiState.Success(metrics = metrics, isOffline = false)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("No pudimos conectar con los sensores. Verifica tu red.")
            }
        }
    }

    fun triggerEmptyState() {
        _uiState.value = HomeUiState.Empty
    }

    fun triggerErrorState() {
        _uiState.value = HomeUiState.Error("No pudimos conectar con los sensores. Por favor reintenta.")
    }
}