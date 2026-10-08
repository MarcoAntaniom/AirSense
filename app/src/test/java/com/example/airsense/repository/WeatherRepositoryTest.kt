package com.example.airsense.repository

import com.example.airsense.data.network.AirQualityCurrentData
import com.example.airsense.data.network.OpenMeteoAirQualityResponse
import com.example.airsense.data.network.OpenMeteoWeatherResponse
import com.example.airsense.data.network.WeatherApiService
import com.example.airsense.data.network.WeatherCurrentData
import com.example.airsense.data.repository.WeatherRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryTest {

    @Test
    fun getAqiStatusString_returnsCorrectCategorization() {
        assertEquals("Excelente", WeatherRepository.getAqiStatusString(20))
        assertEquals("Moderada", WeatherRepository.getAqiStatusString(75))
        assertEquals("Dañina para grupos sensibles", WeatherRepository.getAqiStatusString(120))
        assertEquals("Mala", WeatherRepository.getAqiStatusString(180))
        assertEquals("Peligrosa", WeatherRepository.getAqiStatusString(250))
    }

    @Test
    fun fetchRealtimeMetrics_parsesApiResponseCorrectly() = runBlocking {
        val fakeApiService = object : WeatherApiService {
            override suspend fun getCurrentWeather(
                latitude: Double,
                longitude: Double,
                current: String
            ): OpenMeteoWeatherResponse {
                return OpenMeteoWeatherResponse(
                    current = WeatherCurrentData(
                        temperature2m = 24.8,
                        relativeHumidity2m = 48,
                        weatherCode = 1,
                        windSpeed10m = 12.0
                    )
                )
            }

            override suspend fun getAirQuality(
                latitude: Double,
                longitude: Double,
                current: String
            ): OpenMeteoAirQualityResponse {
                return OpenMeteoAirQualityResponse(
                    current = AirQualityCurrentData(
                        europeanAqi = 15,
                        usAqi = 32,
                        pm25 = 9.4,
                        pm10 = 18.2,
                        carbonMonoxide = 210.0
                    )
                )
            }
        }

        val repository = WeatherRepository(fakeApiService)
        val result = repository.fetchRealtimeMetrics(-33.45, -70.66)

        assertTrue(result.isSuccess)
        val metrics = result.getOrNull()!!
        assertEquals(32, metrics.aqi)
        assertEquals("Excelente", metrics.aqiStatus)
        assertEquals(24.8, metrics.temperatureCelsius, 0.01)
        assertEquals(48, metrics.humidityPercentage)
        assertEquals(9, metrics.pm25Ug)
    }
}
