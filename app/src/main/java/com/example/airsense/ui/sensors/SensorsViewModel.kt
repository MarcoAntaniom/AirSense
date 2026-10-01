package com.example.airsense.ui.sensors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.airsense.data.models.SensorDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SensorsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SensorsUiState>(SensorsUiState.Loading)
    val uiState: StateFlow<SensorsUiState> = _uiState.asStateFlow()

    init {
        loadSensors()
    }

    fun loadSensors() {
        viewModelScope.launch {
            _uiState.value = SensorsUiState.Loading
            delay(800)
            try {
                val mockSensors = listOf(
                    SensorDevice("sns_01", "Sensor Sala Estar", "AQI + Temp", 88, true, "28 AQI"),
                    SensorDevice("sns_02", "Sensor Habitación Principal", "Temp + Humedad", 95, true, "21.8 °C"),
                    SensorDevice("sns_03", "Sensor Oficina IoT", "CO2 + PM2.5", 72, true, "415 ppm"),
                    SensorDevice("sns_04", "Sensor Jardín Exterior", "Estación Clima", 45, false, "Desconectado")
                )
                _uiState.value = SensorsUiState.Success(sensors = mockSensors, isOffline = false)
            } catch (e: Exception) {
                _uiState.value = SensorsUiState.Error("No pudimos conectar con los sensores IoT.")
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