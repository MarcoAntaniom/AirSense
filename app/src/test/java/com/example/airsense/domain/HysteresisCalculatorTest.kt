package com.example.airsense.domain

import com.example.airsense.data.models.AirQualityLevel
import com.example.airsense.data.models.AirQualityThresholds
import org.junit.Assert.assertEquals
import org.junit.Test

class HysteresisCalculatorTest {

    private val calculator = HysteresisCalculator(hysteresisFactor = 0.05)
    private val defaultThresholds = AirQualityThresholds(
        warningPpm = 800,
        dangerPpm = 1200
    )

    @Test
    fun calculateLevel_belowWarning_returnsExcellent() {
        val level = calculator.calculateLevel(
            currentPpm = 300,
            thresholds = defaultThresholds,
            previousLevel = null
        )
        assertEquals(AirQualityLevel.EXCELLENT, level)
    }

    @Test
    fun calculateLevel_reachesWarning_returnsUnhealthySensitive() {
        val level = calculator.calculateLevel(
            currentPpm = 800,
            thresholds = defaultThresholds,
            previousLevel = AirQualityLevel.EXCELLENT
        )
        assertEquals(AirQualityLevel.UNHEALTHY_SENSITIVE, level)
    }

    @Test
    fun calculateLevel_dropsSlightlyWithinHysteresisBand_retainsHazardousLevel() {
        // Danger es 1200 PPM. 5% histéresis límite inferior = 1140 PPM.
        // Si baja a 1180 PPM (dentro de la banda de 5%), se mantiene HAZARDOUS para evitar parpadeo.
        val level = calculator.calculateLevel(
            currentPpm = 1180,
            thresholds = defaultThresholds,
            previousLevel = AirQualityLevel.HAZARDOUS
        )
        assertEquals(AirQualityLevel.HAZARDOUS, level)
    }

    @Test
    fun calculateLevel_dropsBelowHysteresisBand_deescalatesLevel() {
        // Si cae a 1100 PPM (por debajo de 1140 PPM), desescala
        val level = calculator.calculateLevel(
            currentPpm = 1100,
            thresholds = defaultThresholds,
            previousLevel = AirQualityLevel.HAZARDOUS
        )
        assertEquals(AirQualityLevel.UNHEALTHY_SENSITIVE, level)
    }
}
