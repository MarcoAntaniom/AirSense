package com.example.airsense.domain

import com.example.airsense.data.models.AirQualityLevel
import com.example.airsense.data.models.AirQualityThresholds

class HysteresisCalculator(
    private val hysteresisFactor: Double = 0.05 // 5% de banda de histéresis
) {
    /**
     * Evalúa el nivel de calidad del aire a partir de las PPM del sensor de gas MQ,
     * aplicando una banda de histéresis respecto al nivel anterior para evitar parpadeos.
     */
    fun calculateLevel(
        currentPpm: Int,
        thresholds: AirQualityThresholds,
        previousLevel: AirQualityLevel? = null
    ): AirQualityLevel {
        val warningPpm = thresholds.warningPpm
        val dangerPpm = thresholds.dangerPpm

        // Umbrales de histéresis para descenso de nivel (5% más bajo)
        val warningLowerBound = (warningPpm * (1.0 - hysteresisFactor)).toInt()
        val dangerLowerBound = (dangerPpm * (1.0 - hysteresisFactor)).toInt()

        return when (previousLevel) {
            AirQualityLevel.HAZARDOUS -> {
                if (currentPpm < dangerLowerBound) {
                    if (currentPpm < warningLowerBound) AirQualityLevel.EXCELLENT else AirQualityLevel.UNHEALTHY_SENSITIVE
                } else {
                    AirQualityLevel.HAZARDOUS
                }
            }
            AirQualityLevel.UNHEALTHY, AirQualityLevel.UNHEALTHY_SENSITIVE -> {
                when {
                    currentPpm >= dangerPpm -> AirQualityLevel.HAZARDOUS
                    currentPpm < warningLowerBound -> AirQualityLevel.EXCELLENT
                    else -> AirQualityLevel.UNHEALTHY_SENSITIVE
                }
            }
            AirQualityLevel.MODERATE -> {
                when {
                    currentPpm >= dangerPpm -> AirQualityLevel.HAZARDOUS
                    currentPpm >= warningPpm -> AirQualityLevel.UNHEALTHY_SENSITIVE
                    currentPpm < (warningPpm / 2 * (1.0 - hysteresisFactor)).toInt() -> AirQualityLevel.EXCELLENT
                    else -> AirQualityLevel.MODERATE
                }
            }
            AirQualityLevel.EXCELLENT, null -> {
                when {
                    currentPpm >= dangerPpm -> AirQualityLevel.HAZARDOUS
                    currentPpm >= warningPpm -> AirQualityLevel.UNHEALTHY_SENSITIVE
                    currentPpm >= warningPpm / 2 -> AirQualityLevel.MODERATE
                    else -> AirQualityLevel.EXCELLENT
                }
            }
        }
    }
}
