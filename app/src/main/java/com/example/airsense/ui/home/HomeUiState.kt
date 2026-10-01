package com.example.airsense.ui.home

import com.example.airsense.data.models.AirQualityMetrics

sealed interface HomeUiState {
    object Loading : HomeUiState
    
    data class Success(
        val metrics: AirQualityMetrics,
        val isOffline: Boolean = false
    ) : HomeUiState

    object Empty : HomeUiState

    data class Error(
        val message: String
    ) : HomeUiState
}