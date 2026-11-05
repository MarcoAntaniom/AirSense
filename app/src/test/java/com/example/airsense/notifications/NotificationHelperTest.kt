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
            co2 = 1500,
            pm25 = 60.0f
        )
        val shouldSend = NotificationHelper.shouldSendAlert(highRiskMetrics, userNotificationsEnabled = false)
        assertFalse(shouldSend)
    }

    @Test
    fun shouldSendAlert_returnsTrueWhenEnabledAndAQIExceedsThreshold() {
        val elevatedAqiMetrics = AirQualityMetrics(
            aqi = 75,
            aqiStatus = "Moderada",
            co2 = 400,
            pm25 = 10.0f,
            temperature = 22.0f,
            humidity = 50.0f
        )
        val shouldSend = NotificationHelper.shouldSendAlert(elevatedAqiMetrics, userNotificationsEnabled = true)
        assertTrue(shouldSend)
    }

    @Test
    fun shouldSendAlert_returnsTrueWhenEnabledAndCO2ExceedsThreshold() {
        val highCo2Metrics = AirQualityMetrics(
            aqi = 30,
            aqiStatus = "Excelente",
            co2 = 1200,
            pm25 = 10.0f,
            temperature = 22.0f,
            humidity = 50.0f
        )
        val shouldSend = NotificationHelper.shouldSendAlert(highCo2Metrics, userNotificationsEnabled = true)
        assertTrue(shouldSend)
    }

    @Test
    fun shouldSendAlert_returnsTrueWhenTemperatureOutOfBounds() {
        val lowTempMetrics = AirQualityMetrics(
            aqi = 25,
            co2 = 400,
            pm25 = 10.0f,
            temperature = 15.0f, // Bajo 18.0 °C
            humidity = 50.0f
        )
        assertTrue(NotificationHelper.shouldSendAlert(lowTempMetrics, userNotificationsEnabled = true))

        val highTempMetrics = AirQualityMetrics(
            aqi = 25,
            co2 = 400,
            pm25 = 10.0f,
            temperature = 29.0f, // Sobre 26.7 °C TGBH
            humidity = 50.0f
        )
        assertTrue(NotificationHelper.shouldSendAlert(highTempMetrics, userNotificationsEnabled = true))
    }

    @Test
    fun shouldSendAlert_returnsTrueWhenHumidityOutOfBounds() {
        val dryMetrics = AirQualityMetrics(
            aqi = 25,
            co2 = 400,
            pm25 = 10.0f,
            temperature = 22.0f,
            humidity = 20.0f // Bajo 30%
        )
        assertTrue(NotificationHelper.shouldSendAlert(dryMetrics, userNotificationsEnabled = true))

        val humidMetrics = AirQualityMetrics(
            aqi = 25,
            co2 = 400,
            pm25 = 10.0f,
            temperature = 22.0f,
            humidity = 75.0f // Sobre 70%
        )
        assertTrue(NotificationHelper.shouldSendAlert(humidMetrics, userNotificationsEnabled = true))
    }

    @Test
    fun shouldSendAlert_returnsFalseWhenEnabledAndMetricsOptimal() {
        val optimalMetrics = AirQualityMetrics(
            aqi = 25,
            aqiStatus = "Excelente",
            co2 = 420,
            pm25 = 8.0f,
            temperature = 22.4f,
            humidity = 55.0f
        )
        val shouldSend = NotificationHelper.shouldSendAlert(optimalMetrics, userNotificationsEnabled = true)
        assertFalse(shouldSend)
    }

    @Test
    fun createFriendlyNotificationBody_generatesEmpatheticMessageForCo2() {
        val highCo2Metrics = AirQualityMetrics(
            aqi = 25,
            co2 = 1200,
            pm25 = 10.0f,
            temperature = 22.0f,
            humidity = 50.0f
        )
        val body = NotificationHelper.createFriendlyNotificationBody(highCo2Metrics)
        assertTrue(body.contains("1,000 ppm") || body.contains("1200"))
    }

    @Test
    fun createNotificationTitle_generatesCorrectTitles() {
        assertEquals("Ambiente seguro y limpio", NotificationHelper.createNotificationTitle(AlertLevel.GOOD))
        assertEquals("Aviso de Confort Ambient", NotificationHelper.createNotificationTitle(AlertLevel.MODERATE))
        assertEquals("Alerta Ambiental / Laboral", NotificationHelper.createNotificationTitle(AlertLevel.UNHEALTHY))
        assertEquals("🚨 Peligro: Umbral Normativo Superado", NotificationHelper.createNotificationTitle(AlertLevel.HAZARDOUS))
    }

    @Test
    fun createNotificationBody_formatsMetricsCorrectly() {
        val metrics = AirQualityMetrics(
            aqi = 120,
            aqiStatus = "No Saludable",
            co2 = 850,
            co = 2.5f,
            temperature = 22.4f
        )
        val body = NotificationHelper.createNotificationBody(metrics)
        assertTrue(body.contains("850 ppm"))
    }
}
