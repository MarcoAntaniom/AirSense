package com.example.airsense.notifications

import com.example.airsense.data.models.AirQualityMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHelperTest {

    @Test
    fun getAlertLevel_calculatesCorrectLevelForAQI() {
        assertEquals(AlertLevel.GOOD, NotificationHelper.getAlertLevel(25))
        assertEquals(AlertLevel.GOOD, NotificationHelper.getAlertLevel(50))
        assertEquals(AlertLevel.MODERATE, NotificationHelper.getAlertLevel(75))
        assertEquals(AlertLevel.MODERATE, NotificationHelper.getAlertLevel(100))
        assertEquals(AlertLevel.UNHEALTHY, NotificationHelper.getAlertLevel(150))
        assertEquals(AlertLevel.HAZARDOUS, NotificationHelper.getAlertLevel(250))
    }

    @Test
    fun shouldSendAlert_returnsFalseWhenUserNotificationsDisabled() {
        val highRiskMetrics = AirQualityMetrics(
            aqi = 180,
            aqiStatus = "Mala",
            co2Ppm = 1500,
            pm25Ug = 60
        )
        val shouldSend = NotificationHelper.shouldSendAlert(highRiskMetrics, userNotificationsEnabled = false)
        assertFalse(shouldSend)
    }

    @Test
    fun shouldSendAlert_returnsTrueWhenEnabledAndAQIExceedsThreshold() {
        val elevatedAqiMetrics = AirQualityMetrics(
            aqi = 75,
            aqiStatus = "Moderada",
            co2Ppm = 400,
            pm25Ug = 10
        )
        val shouldSend = NotificationHelper.shouldSendAlert(elevatedAqiMetrics, userNotificationsEnabled = true)
        assertTrue(shouldSend)
    }

    @Test
    fun shouldSendAlert_returnsTrueWhenEnabledAndCO2ExceedsThreshold() {
        val highCo2Metrics = AirQualityMetrics(
            aqi = 30,
            aqiStatus = "Excelente",
            co2Ppm = 1200,
            pm25Ug = 10
        )
        val shouldSend = NotificationHelper.shouldSendAlert(highCo2Metrics, userNotificationsEnabled = true)
        assertTrue(shouldSend)
    }

    @Test
    fun shouldSendAlert_returnsFalseWhenEnabledAndMetricsOptimal() {
        val optimalMetrics = AirQualityMetrics(
            aqi = 25,
            aqiStatus = "Excelente",
            co2Ppm = 420,
            pm25Ug = 8
        )
        val shouldSend = NotificationHelper.shouldSendAlert(optimalMetrics, userNotificationsEnabled = true)
        assertFalse(shouldSend)
    }

    @Test
    fun createNotificationTitle_generatesCorrectTitles() {
        assertEquals("Calidad del aire óptima", NotificationHelper.createNotificationTitle(AlertLevel.GOOD))
        assertEquals("Aviso: Calidad del aire moderada", NotificationHelper.createNotificationTitle(AlertLevel.MODERATE))
        assertEquals("Alerta: Calidad del aire no saludable", NotificationHelper.createNotificationTitle(AlertLevel.UNHEALTHY))
        assertEquals("Peligro: Calidad del aire crítica", NotificationHelper.createNotificationTitle(AlertLevel.HAZARDOUS))
    }

    @Test
    fun createNotificationBody_formatsMetricsCorrectly() {
        val metrics = AirQualityMetrics(
            aqi = 120,
            aqiStatus = "No Saludable",
            co2Ppm = 850,
            pm25Ug = 45
        )
        val body = NotificationHelper.createNotificationBody(metrics)
        assertEquals("AQI: 120 (No Saludable) • CO2: 850 ppm • PM2.5: 45 µg/m³", body)
    }
}
