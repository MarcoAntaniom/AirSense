package com.example.airsense.data.repository

import com.example.airsense.data.bluetooth.BluetoothSensorManager
import com.example.airsense.data.models.AirQualityLevel
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.AirQualityThresholds
import com.example.airsense.data.models.AirSenseState
import com.example.airsense.domain.HysteresisCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SensorRepository(
    private val bluetoothSensorManager: BluetoothSensorManager = BluetoothSensorManager(),
    private val hysteresisCalculator: HysteresisCalculator = HysteresisCalculator(),
    private val weatherRepository: WeatherRepository = WeatherRepository()
) {
    companion object {
        const val TTL_MILLIS = 10_000L // Watchdog TTL = 10 segundos
        const val MAX_NAN_ERRORS = 5    // Máximo de fallos seguidos antes de invalidar
    }

    private var lastValidMetrics: AirQualityMetrics? = null
    private var lastValidTimestamp: Long = 0L
    private var consecutiveErrors: Int = 0
    private var previousLevel: AirQualityLevel? = null

    /**
     * Inicia la recolección de datos y emite el estado unificado de la aplicación (AirSenseState),
     * gestionando el periodo de calentamiento, el Watchdog TTL (10s) y la banda de histéresis.
     */
    fun observeSensorState(thresholdsFlow: Flow<AirQualityThresholds>): Flow<AirSenseState> {
        return combine(
            bluetoothSensorManager.startTelemetryStream(),
            thresholdsFlow
        ) { frame, thresholds ->
            val now = System.currentTimeMillis()

            if (frame != null && frame.isValid) {
                consecutiveErrors = 0
                val metrics = bluetoothSensorManager.toAirQualityMetrics(frame)

                // Evaluar nivel con histéresis
                val newLevel = hysteresisCalculator.calculateLevel(
                    currentPpm = metrics.co2Ppm,
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
                    // Retención segura de la última lectura limpia dentro de la ventana TTL
                    AirSenseState.ActiveValid(
                        metrics = cached,
                        level = previousLevel ?: AirQualityLevel.EXCELLENT,
                        isOffline = true
                    )
                } else {
                    // Watchdog TTL Expirado o demasiados errores consecutivos
                    val fallback = cached ?: AirQualityMetrics(lastUpdated = "Sin señal")
                    AirSenseState.Stale(
                        lastValidMetrics = fallback,
                        reason = "Watchdog TTL caducado (>10s sin trama limpia o sensor desconectado)"
                    )
                }
            }
        }
    }

    suspend fun getFallbackMetricsFromApi(): Result<AirQualityMetrics> {
        return weatherRepository.fetchRealtimeMetrics()
    }
}
