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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.data.auth.AuthRepository
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.HttpException
import java.net.UnknownHostException

/**
 * Repositorio de telemetría conectado automáticamente a Google Firebase Realtime Database
 * (https://airsense-c02fa-default-rtdb.firebaseio.com).
 * Obtiene métricas en vivo en /airsense/actual.json e histórico en /airsense/historial.json
 * transmitidos por el microcontrolador ESP8266 vía Internet.
 */
class SensorRepository(
    context: Context? = null,
    private val firebaseSensorApiService: FirebaseSensorApiService? = null,
    private val hysteresisCalculator: HysteresisCalculator = HysteresisCalculator(),
    private val authRepository: AuthRepository = AuthRepository()
) {
    companion object {
        const val POLLING_INTERVAL_MS = 2500L
    }

    private val userPrefs = context?.let { UserPreferencesManager(it) }
    private var lastErrorReason: String? = null

    private val _currentMetrics = MutableStateFlow<AirQualityMetrics?>(null)
    val currentMetrics: StateFlow<AirQualityMetrics?> = _currentMetrics.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionSource = MutableStateFlow(SensorConnectionSource.NONE)
    val connectionSource: StateFlow<SensorConnectionSource> = _connectionSource.asStateFlow()

    private var lastValidMetrics: AirQualityMetrics? = null
    private var previousLevel: AirQualityLevel? = null
    private val defaultFirebaseApiService: FirebaseSensorApiService by lazy {
        FirebaseSensorApiService.create()
    }
    private val refreshTrigger = Channel<Unit>(Channel.CONFLATED)

    private fun getFirebaseApiService(): FirebaseSensorApiService {
        return firebaseSensorApiService ?: defaultFirebaseApiService
    }

    fun requestImmediateRefresh() {
        refreshTrigger.trySend(Unit)
    }

    private suspend fun resolveAuthToken(): String? {
        val prefToken = userPrefs?.firebaseAuthFlow?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
        return prefToken ?: authRepository.getValidIdToken()
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
                val hostPref = userPrefs?.firebaseHostFlow?.firstOrNull()?.trim()
                val defaultHost = FirebaseSensorApiService.FIREBASE_HOST_URL.trimEnd('/')
                val api = firebaseSensorApiService ?: if (!hostPref.isNullOrEmpty() && hostPref.trimEnd('/') != defaultHost) {
                    FirebaseSensorApiService.create(hostPref)
                } else {
                    getFirebaseApiService()
                }

                val token = resolveAuthToken()
                val response = try {
                    api.getLiveMetrics(auth = token)
                } catch (authError: HttpException) {
                    if (authError.code() == 401 && token != null) {
                        api.getLiveMetrics(auth = null)
                    } else {
                        throw authError
                    }
                }
                if (response != null) {
                    metrics = response.toAirQualityMetrics()
                    lastErrorReason = null
                } else {
                    lastErrorReason = "Base de datos vacía (/airsense/actual.json)"
                }
            } catch (e: HttpException) {
                lastErrorReason = if (e.code() == 401) {
                    "Permiso denegado (HTTP 401). Habilita lectura en Reglas de Firebase."
                } else {
                    "Error HTTP ${e.code()} en Firebase"
                }
            } catch (_: UnknownHostException) {
                lastErrorReason = "Sin conexión a Internet"
            } catch (e: Exception) {
                lastErrorReason = e.localizedMessage ?: "Error conectando a Firebase"
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

            withTimeoutOrNull(POLLING_INTERVAL_MS) {
                refreshTrigger.receive()
            }
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
                    reason = lastErrorReason ?: "Sin conexión con Firebase Realtime Database"
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
                val token = resolveAuthToken()
                val response = api.getLiveMetrics(auth = token)
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
                val token = resolveAuthToken()
                val map = api.getHistory(auth = token, limitToLast = limit)
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
