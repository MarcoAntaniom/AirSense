package com.example.airsense.data.repository

import android.content.Context
import com.example.airsense.data.models.AirQualityLevel
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.AirQualityThresholds
import com.example.airsense.data.models.AirSenseState
import com.example.airsense.data.network.LocalSensorApiService
import com.example.airsense.domain.HysteresisCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class SensorRepository(
    private val context: Context? = null,
    private val localSensorApiService: LocalSensorApiService? = null,
    private val hysteresisCalculator: HysteresisCalculator = HysteresisCalculator(),
    private val weatherRepository: WeatherRepository = WeatherRepository()
) {
    companion object {
        const val TTL_MILLIS = 10_000L // Watchdog TTL = 10 segundos
        const val MAX_NAN_ERRORS = 5    // Máximo de fallos seguidos antes de invalidar
        const val POLLING_INTERVAL_MS = 2000L
    }

    private val _currentMetrics = MutableStateFlow<AirQualityMetrics?>(null)
    val currentMetrics: StateFlow<AirQualityMetrics?> = _currentMetrics.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var lastValidMetrics: AirQualityMetrics? = null
    private var lastValidTimestamp: Long = 0L
    private var consecutiveErrors: Int = 0
    private var previousLevel: AirQualityLevel? = null

    private fun getApiService(): LocalSensorApiService {
        return localSensorApiService ?: LocalSensorApiService.create(context)
    }

    /**
     * Realiza un polling periódico en Dispatchers.IO cada 2000 ms a http://192.168.4.1/data.
     * Si la petición es exitosa, emite las métricas y marca isConnected = true.
     * Si lanza excepción (timeout o red no disponible), emite null y marca isConnected = false.
     */
    fun pollTelemetryStream(): Flow<AirQualityMetrics?> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val api = getApiService()
                val response = api.getSensorData()
                val metrics = response.toAirQualityMetrics()
                _isConnected.value = true
                _currentMetrics.value = metrics
                emit(metrics)
            } catch (_: Exception) {
                _isConnected.value = false
                emit(null)
            }
            delay(POLLING_INTERVAL_MS)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Inicia la recolección de datos mediante polling y emite el estado unificado de la aplicación (AirSenseState),
     * gestionando el Watchdog TTL (10s) y la banda de histéresis basada en normativas chilenas.
     */
    fun observeSensorState(thresholdsFlow: Flow<AirQualityThresholds>): Flow<AirSenseState> {
        return combine(
            pollTelemetryStream(),
            thresholdsFlow
        ) { metrics, thresholds ->
            val now = System.currentTimeMillis()

            if (metrics != null && metrics.isValidDht11Reading()) {
                consecutiveErrors = 0
                val newLevel = hysteresisCalculator.calculateMetricsLevel(
                    metrics = metrics,
                    thresholds = thresholds,
                    previousLevel = previousLevel
                )
                previousLevel = newLevel

                val metricsWithStatus = metrics.copy(aqiStatus = newLevel.label)
                lastValidMetrics = metricsWithStatus
                lastValidTimestamp = now

                AirSenseState.ActiveValid(
                    metrics = metricsWithStatus,
                    level = newLevel,
                    isOffline = false
                )
            } else {
                consecutiveErrors++
                val timeSinceLastValid = now - lastValidTimestamp
                val cached = lastValidMetrics

                if (cached != null && consecutiveErrors < MAX_NAN_ERRORS && timeSinceLastValid <= TTL_MILLIS) {
                    AirSenseState.ActiveValid(
                        metrics = cached,
                        level = previousLevel ?: AirQualityLevel.EXCELLENT,
                        isOffline = true
                    )
                } else {
                    val fallback = cached ?: AirQualityMetrics(lastUpdated = "Sin señal")
                    AirSenseState.Stale(
                        lastValidMetrics = fallback,
                        reason = "Sensor WeMos D1 desconectado o sin respuesta HTTP (192.168.4.1)"
                    )
                }
            }
        }
    }

    suspend fun getFallbackMetricsFromApi(): Result<AirQualityMetrics> {
        return weatherRepository.fetchRealtimeMetrics()
    }
}
