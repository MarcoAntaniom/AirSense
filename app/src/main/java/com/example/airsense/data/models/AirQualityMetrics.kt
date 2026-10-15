package com.example.airsense.data.models

/**
 * Representa la lectura de métricas de calidad del aire y sensores ambientales.
 */
data class AirQualityMetrics(
    val aqi: Int = 28,
    val aqiStatus: String = "Excelente",
    val temperatureCelsius: Double = 22.5,
    val humidityPercentage: Int = 54,
    val co2Ppm: Int = 420,
    val pm25Ug: Int = 8,
    val lastUpdated: String = "Ahora mismo",
    val isMawCalibrated: Boolean = true
) {
    /**
     * Valida si las lecturas de temperatura y humedad están dentro de los rangos
     * físicos de la hoja de datos oficial del sensor DHT11 (0°C a 50°C, 20% a 90% HR).
     */
    fun isValidDht11Reading(): Boolean {
        return temperatureCelsius in 0.0..50.0 && humidityPercentage in 20..90
    }

    /**
     * Valida que la concentración de gas esté dentro de un rango físico coherente.
     */
    fun isValidMqReading(): Boolean {
        return co2Ppm in 0..10000
    }
}
