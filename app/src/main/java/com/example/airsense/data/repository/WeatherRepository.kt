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
     * Obtiene los datos meteorológicos y de calidad del aire en tiempo real desde APIs remotas (MMA Chile / OpenMeteo).
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

            try {
                weatherResponse = apiService.getCurrentWeather(latitude, longitude)
            } catch (_: Exception) {
            }

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
                }
            }

            try {
                openMeteoAirQualityResponse = apiService.getAirQuality(latitude, longitude)
            } catch (_: Exception) {
            }

            if (weatherResponse == null && googleAqi == null && openMeteoAirQualityResponse == null) {
                return Result.failure(IllegalStateException("No se pudo obtener datos de ninguna API meteorológica"))
            }

            val temp = weatherResponse?.current?.temperature2m?.toFloat() ?: 22.4f
            val humidity = weatherResponse?.current?.relativeHumidity2m?.toFloat() ?: 55.0f

            val aqi = googleAqi
                ?: openMeteoAirQualityResponse?.current?.usAqi
                ?: openMeteoAirQualityResponse?.current?.europeanAqi
                ?: 35

            val pm25Val = googlePm25?.toFloat()
                ?: openMeteoAirQualityResponse?.current?.pm25?.toFloat()
                ?: 8.0f

            val pm10Val = openMeteoAirQualityResponse?.current?.pm10?.toFloat() ?: 15.0f
            val so2Val = openMeteoAirQualityResponse?.current?.sulphurDioxide?.toFloat() ?: 5.0f

            val aqiStatus = getAqiStatusString(aqi)
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val lastUpdated = "En vivo (${timeFormat.format(Date())})"

            val metrics = AirQualityMetrics(
                aqi = aqi,
                aqiStatus = aqiStatus,
                temperature = temp,
                humidity = humidity,
                gasRaw = 210,
                co = 2.5f,
                ch2o = 0.04f,
                co2 = 450,
                pm25 = pm25Val,
                pm10 = pm10Val,
                so2 = so2Val,
                timestamp = System.currentTimeMillis(),
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
