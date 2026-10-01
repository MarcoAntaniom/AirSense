package com.example.airsense.data.models

data class AirQualityMetrics(
    val aqi: Int = 28,
    val aqiStatus: String = "Excelente",
    val temperatureCelsius: Double = 22.5,
    val humidityPercentage: Int = 54,
    val co2Ppm: Int = 420,
    val pm25Ug: Int = 8,
    val lastUpdated: String = "Ahora mismo"
)