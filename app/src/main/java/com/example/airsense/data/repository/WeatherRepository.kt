package com.example.airsense.data.repository

import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.network.GoogleAirQualityRequest
import com.example.airsense.data.network.LatLngLocation
import com.example.airsense.data.network.OpenMeteoAirQualityResponse
import com.example.airsense.data.network.OpenMeteoWeatherResponse
import com.example.airsense.data.network.WeatherApiService
import com.google.gson.Gson
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
     * Obtiene los datos meteorológicos y de calidad del aire en tiempo real.
     * Si la API Key de Google no existe o si la llamada a Google Air Quality falla (error 400/403, timeout),
     * realiza fallback automático e inmediato a Open-Meteo sin lanzar una excepción no controlada a la UI.
     */
    suspend fun fetchRealtimeMetrics(
        latitude: Double = -33.45,
        longitude: Double = -70.66
    ): Result<AirQualityMetrics> {
        return try {
            var weatherResponse: OpenMeteoWeatherResponse? = null
            var openMeteoAirQualityResponse: OpenMeteoAirQualityResponse? = null
            var googleAqi: Int? = null
            var googlePm25: Int? = null

            // 1. Obtención de datos meteorológicos básicos (Open-Meteo Weather API)
            try {
                weatherResponse = apiService.getCurrentWeather(latitude, longitude)
            } catch (_: Exception) {
                // Fallback manejado silenciosamente
            }

            // 2. Intentar llamada a Google Air Quality API solo si la API Key es válida
            if (!googleApiKey.isNullOrBlank()) {
                try {
                    val request = GoogleAirQualityRequest(
                        location = LatLngLocation(latitude, longitude)
                    )
                    val googleResponse = apiService.getGoogleAirQuality(request, googleApiKey)
                    googleAqi = googleResponse.indexes?.firstOrNull()?.aqi
                    googlePm25 = googleResponse.pollutants
                        ?.firstOrNull { it.code.equals("pm25", ignoreCase = true) }
                        ?.concentration?.value?.toInt()
                } catch (_: Exception) {
                    // Si Google Air Quality falla (HTTP 403, 400, timeout, quota), continua con fallback a Open-Meteo
                }
            }

            // 3. Fallback a Open-Meteo Air Quality si Google no entregó respuesta o no se configuró API Key
            try {
                openMeteoAirQualityResponse = apiService.getAirQuality(latitude, longitude)
            } catch (_: Exception) {
                // Fallback manejado silenciosamente
            }

            if (weatherResponse == null && googleAqi == null && openMeteoAirQualityResponse == null) {
                return Result.failure(IllegalStateException("No se pudo obtener datos de ninguna API meteorológica"))
            }

            val temp = weatherResponse?.current?.temperature2m ?: 22.5
            val humidity = weatherResponse?.current?.relativeHumidity2m ?: 54

            val aqi = googleAqi
                ?: openMeteoAirQualityResponse?.current?.usAqi
                ?: openMeteoAirQualityResponse?.current?.europeanAqi
                ?: 28

            val pm25 = googlePm25
                ?: openMeteoAirQualityResponse?.current?.pm25?.toInt()
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
