package com.example.airsense.data.models

import com.google.gson.annotations.SerializedName

/**
 * Representa la lectura unificada de métricas ambientales interiores (WeMos D1)
 * y calidad de aire exterior (MMA Chile / APIs meteorológicas).
 */
data class AirQualityMetrics(
    @SerializedName("aqi")
    val aqi: Int = 35,

    @SerializedName("aqiStatus")
    val aqiStatus: String = "Excelente",

    @SerializedName("temperature")
    val temperature: Float = 22.4f,

    @SerializedName("humidity")
    val humidity: Float = 55.0f,

    @SerializedName("gasRaw")
    val gasRaw: Int = 210,

    @SerializedName("co")
    val co: Float = 2.5f,

    @SerializedName("ch2o")
    val ch2o: Float = 0.04f,

    @SerializedName("co2")
    val co2: Int = 450,

    @SerializedName("pm25")
    val pm25: Float = 8.0f,

    @SerializedName("pm10")
    val pm10: Float = 15.0f,

    @SerializedName("so2")
    val so2: Float = 5.0f,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    val lastUpdated: String = "Ahora mismo",
    val isMawCalibrated: Boolean = true
) {
    /**
     * Adaptador de temperatura a Double para retrocompatibilidad.
     */
    val temperatureCelsius: Double
        get() = temperature.toDouble()

    /**
     * Adaptador de humedad a Int para retrocompatibilidad.
     */
    val humidityPercentage: Int
        get() = humidity.toInt()

    /**
     * Adaptador de CO2 a Int para retrocompatibilidad.
     */
    val co2Ppm: Int
        get() = co2

    /**
     * Adaptador de PM2.5 a Int para retrocompatibilidad.
     */
    val pm25Ug: Int
        get() = pm25.toInt()

    /**
     * Valida si las lecturas de temperatura y humedad están dentro de los rangos
     * físicos de la hoja de datos oficial del sensor DHT11 (0°C a 50°C, 20% a 90% HR).
     */
    fun isValidDht11Reading(): Boolean {
        return temperature in 0.0f..50.0f && humidity in 20.0f..90.0f
    }

    /**
     * Valida que la concentración de gas/CO2 esté dentro de un rango físico coherente.
     */
    fun isValidMqReading(): Boolean {
        return co2 in 0..10000
    }
}
