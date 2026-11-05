package com.example.airsense.data.models

/**
 * Umbrales de evaluación ambiental basados en la normativa chilena de ambientes laborales
 * (D.S. 594 - LPP, LPA, TGBH) y las Normas Primarias de Calidad del Aire Exterior (MMA Chile).
 */
data class AirQualityThresholds(
    // Monóxido de Carbono (CO) - Normativa Laboral Chile (LPP: 40 ppm)
    val coWarningPpm: Float = CO_WARNING_PPM,
    val coDangerPpm: Float = CO_DANGER_PPM,

    // Formaldehído (CH2O) - Normativa Laboral Chile (LPA: 0.30 ppm)
    val ch2oWarningPpm: Float = CH2O_WARNING_PPM,
    val ch2oDangerPpm: Float = CH2O_DANGER_PPM,

    // Dióxido de Carbono (CO2) - Alarma de Ventilación
    val co2OptimalPpm: Int = CO2_OPTIMAL_MAX,
    val co2DangerPpm: Int = CO2_DANGER_PPM,

    // Temperatura Ambiente Laboral (TGBH Trabajo Moderado)
    val tempWarningC: Float = TEMP_TGBH_WARNING_C,
    val tempDangerC: Float = TEMP_TGBH_DANGER_C,

    // Material Particulado Fino (PM2.5) - MMA Chile
    val pm25NormalMax: Float = PM25_NORMAL_MAX,
    val pm25EmergencyMin: Float = PM25_EMERGENCY_MIN,

    // Material Particulado Grueso (PM10) - MMA Chile
    val pm10NormalMax: Float = PM10_NORMAL_MAX,
    val pm10EmergencyMin: Float = PM10_EMERGENCY_MIN,

    // Dióxido de Azufre (SO2) - MMA Chile
    val so2NormalMax: Float = SO2_NORMAL_MAX,
    val so2DangerMin: Float = SO2_DANGER_MIN,

    // Compatibilidad con versiones previas y UserPreferencesManager
    val warningPpm: Int = CO2_OPTIMAL_MAX,
    val dangerPpm: Int = CO2_DANGER_PPM,
    val maxTemperatureCelsius: Double = 35.0,
    val minHumidityPercentage: Int = 30,
    val maxHumidityPercentage: Int = 70
) {
    companion object {
        // Monóxido de Carbono (CO) - Normativa Chilena LPP
        const val CO_NORMAL_MAX = 15.0f
        const val CO_WARNING_PPM = 15.0f
        const val CO_DANGER_PPM = 40.0f // LPP

        // Formaldehído (CH2O) - Normativa Chilena LPA
        const val CH2O_NORMAL_MAX = 0.10f
        const val CH2O_WARNING_PPM = 0.10f
        const val CH2O_DANGER_PPM = 0.30f // LPA

        // Dióxido de Carbono (CO2) - Confort y Alarma de Ventilación
        const val CO2_OPTIMAL_MAX = 800
        const val CO2_ACCEPTABLE_MAX = 1000
        const val CO2_DANGER_PPM = 1000 // Alarma de ventilación

        // Temperatura (Trabajo Moderado TGBH - D.S. 594 Chile)
        const val TEMP_TGBH_NORMAL_MAX = 24.5f
        const val TEMP_TGBH_WARNING_C = 24.5f
        const val TEMP_TGBH_DANGER_C = 26.7f // Límite TGBH

        // Outdoor PM2.5 (µg/m³ 24h) - Norma Primaria MMA Chile
        const val PM25_NORMAL_MAX = 38.0f
        const val PM25_EMERGENCY_MIN = 170.0f

        // Outdoor PM10 (µg/m³ 24h) - Norma Primaria MMA Chile
        const val PM10_NORMAL_MAX = 150.0f
        const val PM10_EMERGENCY_MIN = 500.0f

        // Outdoor SO2 (µg/m³ Límite Horario) - Norma Primaria MMA Chile
        const val SO2_NORMAL_MAX = 150.0f
        const val SO2_DANGER_MIN = 350.0f
    }

    fun isValid(): Boolean {
        return coWarningPpm < coDangerPpm &&
                ch2oWarningPpm < ch2oDangerPpm &&
                tempWarningC < tempDangerC &&
                minHumidityPercentage in 0..maxHumidityPercentage - 1 &&
                maxHumidityPercentage <= 100
    }
}
