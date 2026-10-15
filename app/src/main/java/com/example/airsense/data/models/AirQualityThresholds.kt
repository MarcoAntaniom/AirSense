package com.example.airsense.data.models

/**
 * Configuración de umbrales de calidad del aire definidos por el usuario o por defecto.
 */
data class AirQualityThresholds(
    val warningPpm: Int = 800,
    val dangerPpm: Int = 1200,
    val maxTemperatureCelsius: Double = 35.0,
    val minHumidityPercentage: Int = 30,
    val maxHumidityPercentage: Int = 70
) {
    /**
     * Valida el orden estricto de los umbrales.
     */
    fun isValid(): Boolean {
        return warningPpm in 1..dangerPpm - 1 &&
                minHumidityPercentage in 0..maxHumidityPercentage - 1 &&
                maxHumidityPercentage <= 100
    }
}
