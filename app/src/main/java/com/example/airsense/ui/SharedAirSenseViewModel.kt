package com.example.airsense.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.AirQualityThresholds
import com.example.airsense.data.models.AirSenseState
import com.example.airsense.data.models.SensorDevice
import com.example.airsense.data.repository.SensorRepository
import com.example.airsense.data.repository.SensorsRepository
import com.example.airsense.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TourStep {
    NONE, HOME, SENSORS, SENSOR_DETAIL, RECOMMENDATIONS, SETTINGS
}

class SharedAirSenseViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val userPrefs = UserPreferencesManager(application)
    private val sensorRepository = SensorRepository()
    private val sensorsRepository = SensorsRepository()
    private val weatherRepository = WeatherRepository()

    private val _selectedSensor = MutableStateFlow<SensorDevice?>(null)
    val selectedSensor: StateFlow<SensorDevice?> = _selectedSensor.asStateFlow()

    private val _airQualityMetrics = MutableStateFlow<AirQualityMetrics?>(null)
    val airQualityMetrics: StateFlow<AirQualityMetrics?> = _airQualityMetrics.asStateFlow()

    private val _currentTourStep = MutableStateFlow(TourStep.NONE)
    val currentTourStep: StateFlow<TourStep> = _currentTourStep.asStateFlow()

    val sensors: StateFlow<List<SensorDevice>> = sensorsRepository.sensors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val thresholds: StateFlow<AirQualityThresholds> = userPrefs.thresholdsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AirQualityThresholds()
        )

    val airSenseState: StateFlow<AirSenseState> = sensorRepository
        .observeSensorState(userPrefs.thresholdsFlow)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AirSenseState.Connecting
        )

    init {
        viewModelScope.launch {
            airSenseState.collect { state ->
                when (state) {
                    is AirSenseState.ActiveValid -> {
                        val sanitizedMetrics = sensorsRepository.sanitizeMetrics(state.metrics)
                        _airQualityMetrics.value = sanitizedMetrics
                        sensorsRepository.generateSensorsFromMetrics(sanitizedMetrics, isOnline = true)
                    }
                    is AirSenseState.Stale -> {
                        val sanitizedMetrics = sensorsRepository.sanitizeMetrics(state.lastValidMetrics)
                        _airQualityMetrics.value = sanitizedMetrics
                        sensorsRepository.generateSensorsFromMetrics(sanitizedMetrics, isOnline = false)
                    }
                    else -> {}
                }
            }
        }
    }

    fun startTour() {
        _currentTourStep.value = TourStep.HOME
        viewModelScope.launch {
            userPrefs.setFirstRunCompleted(false)
        }
    }

    fun setTourStep(step: TourStep) {
        _currentTourStep.value = step
    }

    fun completeTour() {
        _currentTourStep.value = TourStep.NONE
        viewModelScope.launch {
            userPrefs.setFirstRunCompleted(true)
        }
    }

    fun cancelTour() {
        _currentTourStep.value = TourStep.NONE
    }

    fun updateMetrics(metrics: AirQualityMetrics) {
        val sanitized = sensorsRepository.sanitizeMetrics(metrics)
        _airQualityMetrics.value = sanitized
        sensorsRepository.generateSensorsFromMetrics(sanitized, isOnline = true)
    }

    fun selectSensor(sensor: SensorDevice) {
        _selectedSensor.value = sensorsRepository.sanitizeSensor(sensor)
    }

    fun updateThresholds(newThresholds: AirQualityThresholds, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val saved = userPrefs.saveThresholds(newThresholds)
            onResult(saved)
        }
    }

    fun fetchRealtimeWeather(latitude: Double = -33.45, longitude: Double = -70.66) {
        viewModelScope.launch {
            val result = weatherRepository.fetchRealtimeMetrics(latitude, longitude)
            result.onSuccess { metrics ->
                updateMetrics(metrics)
                weatherRepository.saveCachedMetrics(userPrefs, metrics)
            }.onFailure {
                val cached = weatherRepository.getCachedMetrics(userPrefs)
                if (cached != null) {
                    updateMetrics(cached)
                }
            }
        }
    }
}