package com.example.airsense.ui.sensors

import com.example.airsense.data.models.SensorDevice

sealed interface SensorsUiState {
    object Loading : SensorsUiState

    data class Success(
        val sensors: List<SensorDevice>,
        val isOffline: Boolean = false
    ) : SensorsUiState

    object Empty : SensorsUiState

    data class Error(
        val message: String
    ) : SensorsUiState
}