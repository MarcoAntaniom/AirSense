package com.example.airsense.data.repository

import android.content.Context
import com.example.airsense.data.models.AirQualityLevel
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.AirQualityThresholds
import com.example.airsense.data.models.AirSenseState
import com.example.airsense.data.models.SensorConnectionSource
import com.example.airsense.data.network.FirebaseSensorApiService
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
import kotlinx.coroutines.withContext

/**
 * Repositorio de telemetría conectado automáticamente a Google Firebase Realtime Database
 * (https://airsense-c02fa-default-rtdb.firebaseio.com).
 * Obtiene métricas en vivo en /airsense/actual.json e histórico en /airsense/historial.json
 * transmitidos por el microcontrolador ESP8266 vía Internet.
 */
class SensorRepository(
    @Suppress("unused") private val context: Context? = null,
    private val firebaseSensorApiService: FirebaseSensorApiService? = null,
    private val hysteresisCalculator: HysteresisCalculator = HysteresisCalculator()
) {
    companion object {
        const val POLLING_INTERVAL_MS = 2500L
    }

    private val _currentMetrics = MutableStateFlow<AirQualityMetrics?>(null)
    val currentMetrics: StateFlow<AirQualityMetrics?> = _currentMetrics.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionSource = MutableStateFlow(SensorConnectionSource.NONE)
    val connectionSource: StateFlow<SensorConnectionSource> = _connectionSource.asStateFlow()

    private var lastValidMetrics: AirQualityMetrics? = null
    private var previousLevel: AirQualityLevel? = null

    private fun getFirebaseApiService(): FirebaseSensorApiService {
        return firebaseSensorApiService ?: FirebaseSensorApiService.create()
    }

    /**
     * Realiza un polling periódico cada 2500 ms directamente a Google Firebase Realtime Database.
     * Si la petición a /airsense/actual.json es exitosa, emite las métricas y marca isConnected = true.
     * Si falla o no hay internet, emite null y marca isConnected = false.
     */
    fun pollTelemetryStream(): Flow<AirQualityMetrics?> = flow {
        while (currentCoroutineContext().isActive) {
            var metrics: AirQualityMetrics? = null

            try {
                val fbApi = getFirebaseApiService()
                val response = fbApi.getLiveMetrics()
                if (response != null) {
                    metrics = response.toAirQualityMetrics()
                }
            } catch (_: Exception) {
                // Sin conexión a Firebase
            }

            if (metrics != null) {
                _isConnected.value = true
                _connectionSource.value = SensorConnectionSource.FIREBASE_CLOUD
                _currentMetrics.value = metrics
                emit(metrics)
            } else {
                _isConnected.value = false
                _connectionSource.value = SensorConnectionSource.NONE
                emit(null)
            }

            delay(POLLING_INTERVAL_MS)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Observa el flujo de telemetría de Firebase y calcula el estado reactivo AirSenseState.
     */
    fun observeSensorState(thresholdsFlow: Flow<AirQualityThresholds>): Flow<AirSenseState> {
        return combine(
            pollTelemetryStream(),
            thresholdsFlow
        ) { metrics, thresholds ->
            if (metrics != null && metrics.isValidDht11Reading()) {
                val newLevel = hysteresisCalculator.calculateMetricsLevel(
                    metrics = metrics,
                    thresholds = thresholds,
                    previousLevel = previousLevel
                )
                previousLevel = newLevel

                val metricsWithStatus = metrics.copy(aqiStatus = newLevel.label)
                lastValidMetrics = metricsWithStatus

                AirSenseState.ActiveValid(
                    metrics = metricsWithStatus,
                    level = newLevel
                )
            } else {
                val fallback = lastValidMetrics ?: AirQualityMetrics(lastUpdated = "Sin señal")
                AirSenseState.Stale(
                    lastValidMetrics = fallback,
                    reason = "Sin conexión con Firebase Realtime Database"
                )
            }
        }
    }

    suspend fun testFirebaseDirectly(rawHostUrl: String? = null): Result<AirQualityMetrics> {
        return withContext(Dispatchers.IO) {
            try {
                val api = firebaseSensorApiService ?: if (rawHostUrl != null) {
                    FirebaseSensorApiService.create(rawHostUrl)
                } else {
                    getFirebaseApiService()
                }
                val response = api.getLiveMetrics()
                if (response != null) {
                    Result.success(response.toAirQualityMetrics())
                } else {
                    Result.failure(Exception("La base de datos respondió, pero '/airsense/actual.json' no contiene datos aún."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun fetchHistoryFromFirebase(limit: Int = 30): Result<List<AirQualityMetrics>> {
        return withContext(Dispatchers.IO) {
            try {
                val api = getFirebaseApiService()
                val map = api.getHistory(limitToLast = limit)
                if (map.isNullOrEmpty()) {
                    Result.success(emptyList())
                } else {
                    val list = map.values
                        .map { it.toAirQualityMetrics() }
                        .sortedBy { it.timestamp }
                    Result.success(list)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
