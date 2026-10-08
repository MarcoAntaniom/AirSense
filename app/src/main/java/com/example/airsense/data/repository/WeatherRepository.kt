package com.example.airsense.data.repository

import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.network.GoogleAirQualityRequest
import com.example.airsense.data.network.LatLngLocation
import com.example.airsense.data.network.WeatherApiService
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherRepository(
    private val apiService: WeatherApiService = WeatherApiService.create(),
    private val googleApiKey: String? = null,
    private val gson: Gson = Gson()
) {

    /**
     * Guarda la lectura de métricas en el almacenamiento persistente para acceso offline.
     */
    suspend fun saveCachedMetrics(userPrefs: UserPreferencesManager, metrics: AirQualityMetrics) {
        try {
            val json = gson.toJson(metrics)
            userPrefs.saveCachedTelemetry(json)
        } catch (_: Exception) {
        }
    }

    /**
     * Recupera las últimas métricas almacenadas en caché.
     */
    suspend fun getCachedMetrics(userPrefs: UserPreferencesManager): AirQualityMetrics? {
        return try {
            val json = userPrefs.cachedTelemetryFlow.first()
            if (json.isNotBlank()) {
                val cached = gson.fromJson(json, AirQualityMetrics::class.java)
                val rawTime = cached.lastUpdated.replace("En vivo ", "").replace("Modo Local", "")
                cached.copy(lastUpdated = "Último registro guardado ($rawTime)")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Obtiene los datos meteorológicos y de calidad del aire en tiempo real desde la API de Google / Clima.
     * Por defecto utiliza coordenadas de Santiago (-33.45, -70.66) si no se proveen.
     */
    suspend fun fetchRealtimeMetrics(
        latitude: Double = -33.45,
        longitude: Double = -70.66
    ): Result<AirQualityMetrics> {
        return try {
            coroutineScope {
                val weatherDeferred = async { apiService.getCurrentWeather(latitude, longitude) }
                
                val googleAirQualityDeferred = if (!googleApiKey.isNullOrBlank()) {
                    async {
                        try {
                            val request = GoogleAirQualityRequest(
                                location = LatLngLocation(latitude, longitude)
                            )
                            apiService.getGoogleAirQuality(request, googleApiKey)
                        } catch (_: Exception) {
                            null
                        }
                    }
                } else null

                val airQualityDeferred = async { apiService.getAirQuality(latitude, longitude) }

                val weatherResponse = weatherDeferred.await()
                val googleAirQualityResponse = googleAirQualityDeferred?.await()
                val airQualityResponse = airQualityDeferred.await()

                val temp = weatherResponse.current?.temperature2m ?: 22.5
                val humidity = weatherResponse.current?.relativeHumidity2m ?: 54
                
                val aqi = googleAirQualityResponse?.indexes?.firstOrNull()?.aqi
                    ?: airQualityResponse.current?.usAqi
                    ?: airQualityResponse.current?.europeanAqi
                    ?: 28

                val googlePm25 = googleAirQualityResponse?.pollutants
                    ?.firstOrNull { it.code.equals("pm25", ignoreCase = true) }
                    ?.concentration?.value?.toInt()

                val pm25 = googlePm25
                    ?: airQualityResponse.current?.pm25?.toInt()
                    ?: 8

                val aqiStatus = getAqiStatusString(aqi)
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val lastUpdated = "En vivo (${timeFormat.format(Date())})"

                val metrics = AirQualityMetrics(
                    aqi = aqi,
                    aqiStatus = aqiStatus,
                    temperatureCelsius = temp,
                    humidityPercentage = humidity,
                    co2Ppm = 420,
                    pm25Ug = pm25,
                    lastUpdated = lastUpdated
                )

                Result.success(metrics)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun getAqiStatusString(aqi: Int): String {
            return when {
                aqi <= 50 -> "Excelente"
                aqi <= 100 -> "Moderada"
                aqi <= 150 -> "Dañina para grupos sensibles"
                aqi <= 200 -> "Mala"
                else -> "Peligrosa"
            }
        }
    }
}
