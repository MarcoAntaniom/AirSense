package com.example.airsense.domain

import com.example.airsense.data.models.AirQualityLevel
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.AirQualityThresholds

class HysteresisCalculator(
    private val hysteresisFactor: Double = 0.05 // 5% de banda de histéresis
) {
    /**
     * Evalúa el nivel de calidad del aire a partir de las PPM del sensor de gas MQ o CO2,
     * aplicando una banda de histéresis respecto al nivel anterior para evitar parpadeos.
     */
    fun calculateLevel(
        currentPpm: Int,
        thresholds: AirQualityThresholds,
        previousLevel: AirQualityLevel? = null
    ): AirQualityLevel {
        val warningPpm = thresholds.warningPpm
        val dangerPpm = thresholds.dangerPpm

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

    /**
     * Evalúa la totalidad de las métricas ambientales (interiores y exteriores)
     * contrastadas con los umbrales normativos chilenos (LPP, LPA, TGBH, MMA Chile).
     */
    fun calculateMetricsLevel(
        metrics: AirQualityMetrics,
        thresholds: AirQualityThresholds = AirQualityThresholds(),
        previousLevel: AirQualityLevel? = null
    ): AirQualityLevel {
        val factor = 1.0 - hysteresisFactor

        // Umbrales de Peligro/Emergencia (LPP, LPA, TGBH, MMA Chile)
        val isHazardousNow = metrics.co > thresholds.coDangerPpm ||
                metrics.ch2o > thresholds.ch2oDangerPpm ||
                metrics.co2 > thresholds.co2DangerPpm ||
                metrics.temperature > thresholds.tempDangerC ||
                metrics.pm25 >= thresholds.pm25EmergencyMin ||
                metrics.pm10 >= thresholds.pm10EmergencyMin ||
                metrics.so2 > thresholds.so2DangerMin

        // Umbrales de Peligro con Histéresis (5% más bajo para desescalar)
        val isHazardousRetained = metrics.co >= (thresholds.coDangerPpm * factor).toFloat() ||
                metrics.ch2o >= (thresholds.ch2oDangerPpm * factor).toFloat() ||
                metrics.co2 >= (thresholds.co2DangerPpm * factor).toInt() ||
                metrics.temperature >= (thresholds.tempDangerC * factor).toFloat() ||
                metrics.pm25 >= (thresholds.pm25EmergencyMin * factor).toFloat() ||
                metrics.pm10 >= (thresholds.pm10EmergencyMin * factor).toFloat() ||
                metrics.so2 >= (thresholds.so2DangerMin * factor).toFloat()

        if (previousLevel == AirQualityLevel.HAZARDOUS) {
            if (isHazardousRetained) return AirQualityLevel.HAZARDOUS
        } else if (isHazardousNow) {
            return AirQualityLevel.HAZARDOUS
        }

        // Umbrales de Precaución/Alerta
        val isWarningNow = metrics.co > thresholds.coWarningPpm ||
                metrics.ch2o > thresholds.ch2oWarningPpm ||
                metrics.co2 > thresholds.co2OptimalPpm ||
                metrics.temperature > thresholds.tempWarningC ||
                metrics.pm25 > thresholds.pm25NormalMax ||
                metrics.pm10 > thresholds.pm10NormalMax ||
                metrics.so2 > thresholds.so2NormalMax

        val isWarningRetained = metrics.co >= (thresholds.coWarningPpm * factor).toFloat() ||
                metrics.ch2o >= (thresholds.ch2oWarningPpm * factor).toFloat() ||
                metrics.co2 >= (thresholds.co2OptimalPpm * factor).toInt() ||
                metrics.temperature >= (thresholds.tempWarningC * factor).toFloat() ||
                metrics.pm25 >= (thresholds.pm25NormalMax * factor).toFloat() ||
                metrics.pm10 >= (thresholds.pm10NormalMax * factor).toFloat() ||
                metrics.so2 >= (thresholds.so2NormalMax * factor).toFloat()

        return if (previousLevel == AirQualityLevel.UNHEALTHY || previousLevel == AirQualityLevel.UNHEALTHY_SENSITIVE) {
            if (isWarningRetained) AirQualityLevel.UNHEALTHY_SENSITIVE else AirQualityLevel.EXCELLENT
        } else if (isWarningNow) {
            AirQualityLevel.UNHEALTHY_SENSITIVE
        } else {
            AirQualityLevel.EXCELLENT
        }
    }
}
