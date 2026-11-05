package com.example.airsense.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.airsense.MainActivity
import com.example.airsense.R
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.AirQualityThresholds
import kotlinx.coroutines.flow.first

enum class AlertLevel {
    GOOD,
    MODERATE,
    UNHEALTHY,
    HAZARDOUS
}

/**
 * Módulo de gestión de notificaciones para la aplicación AirSense.
 * Configurado según la normativa chilena de ambientes laborales (LPP, LPA, TGBH)
 * y Normas Primarias de Calidad del Aire Exterior (MMA Chile).
 */
class NotificationHelper(private val context: Context) {

    var cooldownIntervalMillis: Long = DEFAULT_COOLDOWN_MILLIS
    var lastNotificationTimeMillis: Long = 0L
    var lastNotifiedLevel: AlertLevel = AlertLevel.GOOD

    companion object {
        const val CHANNEL_ID = "airsense_alerts_channel"
        const val CHANNEL_NAME = "Alertas de Calidad del Aire y Salud Laboral"
        const val CHANNEL_DESCRIPTION = "Notificaciones sobre riesgos ambientales, normativas LPP/LPA/TGBH y MMA Chile."
        const val NOTIFICATION_ID_ALERT = 1001
        const val NOTIFICATION_ID_TEST = 1002

        const val DEFAULT_COOLDOWN_MILLIS = 15 * 60 * 1000L

        const val TEMP_MIN_NORMAL = 18.0f
        const val HUMIDITY_MIN_NORMAL = 30.0f
        const val HUMIDITY_MAX_NORMAL = 60.0f
        const val AQI_THRESHOLD = 50

        fun getAlertLevel(aqi: Int): AlertLevel {
            return when {
                aqi <= 50 -> AlertLevel.GOOD
                aqi <= 100 -> AlertLevel.MODERATE
                aqi <= 200 -> AlertLevel.UNHEALTHY
                else -> AlertLevel.HAZARDOUS
            }
        }

        fun shouldSendAlert(metrics: AirQualityMetrics, userNotificationsEnabled: Boolean): Boolean {
            if (!userNotificationsEnabled) return false

            val isCoDanger = metrics.co > AirQualityThresholds.CO_WARNING_PPM
            val isCh2oDanger = metrics.ch2o > AirQualityThresholds.CH2O_WARNING_PPM
            val isCo2Danger = metrics.co2 > AirQualityThresholds.CO2_OPTIMAL_MAX
            val isTempOutOfBounds = metrics.temperature < TEMP_MIN_NORMAL || metrics.temperature > AirQualityThresholds.TEMP_TGBH_WARNING_C
            val isHumidityOutOfBounds = metrics.humidity < HUMIDITY_MIN_NORMAL || metrics.humidity > HUMIDITY_MAX_NORMAL
            val isPm25Elevated = metrics.pm25 > AirQualityThresholds.PM25_NORMAL_MAX
            val isPm10Elevated = metrics.pm10 > AirQualityThresholds.PM10_NORMAL_MAX
            val isSo2Elevated = metrics.so2 > AirQualityThresholds.SO2_NORMAL_MAX
            val isAqiElevated = metrics.aqi > AQI_THRESHOLD

            return isCoDanger || isCh2oDanger || isCo2Danger || isTempOutOfBounds ||
                    isHumidityOutOfBounds || isPm25Elevated || isPm10Elevated || isSo2Elevated || isAqiElevated
        }

        fun createNotificationTitle(alertLevel: AlertLevel): String {
            return when (alertLevel) {
                AlertLevel.GOOD -> "Ambiente seguro y limpio"
                AlertLevel.MODERATE -> "Aviso de Confort Ambient"
                AlertLevel.UNHEALTHY -> "Alerta Ambiental / Laboral"
                AlertLevel.HAZARDOUS -> "🚨 Peligro: Umbral Normativo Superado"
            }
        }

        fun createNotificationBody(metrics: AirQualityMetrics): String {
            return "AQI: ${metrics.aqi} (${metrics.aqiStatus}) • CO2: ${metrics.co2} ppm • PM2.5: ${metrics.pm25.toInt()} µg/m³"
        }

        fun createFriendlyNotificationTitle(metrics: AirQualityMetrics): String {
            return when {
                metrics.co > AirQualityThresholds.CO_DANGER_PPM -> "🚨 Peligro: LPP Monóxido de Carbono Superado"
                metrics.ch2o > AirQualityThresholds.CH2O_DANGER_PPM -> "🚨 Peligro: LPA Formaldehído Superado"
                metrics.co2 > AirQualityThresholds.CO2_DANGER_PPM -> "🌬️ Alerta CO₂: Ventilación Inmediata Requerida"
                metrics.temperature > AirQualityThresholds.TEMP_TGBH_DANGER_C -> "🌡️ Peligro Térmico: Límite TGBH Superado"
                metrics.pm25 >= AirQualityThresholds.PM25_EMERGENCY_MIN -> "🚨 Emergencia Ambiental Exterior (PM2.5)"
                metrics.pm10 >= AirQualityThresholds.PM10_EMERGENCY_MIN -> "🚨 Emergencia Ambiental Exterior (PM10)"
                metrics.so2 > AirQualityThresholds.SO2_DANGER_MIN -> "⚠️ Alerta SO₂ Exterior Elevado"
                else -> "Consejo de Confort AirSense"
            }
        }

        fun createFriendlyNotificationBody(metrics: AirQualityMetrics): String {
            val alerts = mutableListOf<String>()

            if (metrics.co > AirQualityThresholds.CO_DANGER_PPM) {
                alerts.add("CO alcanza ${metrics.co} ppm (Supera LPP 40 ppm). Evacuar o ventilar el área.")
            }
            if (metrics.ch2o > AirQualityThresholds.CH2O_DANGER_PPM) {
                alerts.add("CH₂O alcanza ${metrics.ch2o} ppm (Supera LPA 0.30 ppm). Renovar el aire del recinto.")
            }
            if (metrics.co2 > AirQualityThresholds.CO2_DANGER_PPM) {
                alerts.add("CO₂ alcanza ${metrics.co2} ppm (Supera 1,000 ppm). Activar ventilación forzada.")
            }
            if (metrics.temperature > AirQualityThresholds.TEMP_TGBH_DANGER_C) {
                alerts.add("La temperatura ha bajado (${metrics.temperature}°C) o superado el límite TGBH (${metrics.temperature}°C). Abrígate bien o ajusta la climatización.")
            } else if (metrics.temperature < TEMP_MIN_NORMAL) {
                alerts.add("La temperatura ha bajado (${metrics.temperature}°C). Abrígate bien o ajusta la calefacción.")
            }
            if (metrics.humidity < HUMIDITY_MIN_NORMAL) {
                alerts.add("Parece que el ambiente está un poco seco (${metrics.humidity.toInt()}%). Considera ventilar la habitación o beber agua.")
            } else if (metrics.humidity > HUMIDITY_MAX_NORMAL) {
                alerts.add("La humedad está algo elevada (${metrics.humidity.toInt()}%). Te recomendamos ventilar la habitación para renovar el aire.")
            }
            if (metrics.pm25 >= AirQualityThresholds.PM25_EMERGENCY_MIN) {
                alerts.add("PM2.5 exterior en nivel Crítico (${metrics.pm25} µg/m³ >= 170). Mantener ventanas cerradas.")
            }

            return if (alerts.isNotEmpty()) {
                alerts.joinToString(" ")
            } else {
                createNotificationBody(metrics)
            }
        }
    }

    fun hasPostNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun isCooldownActive(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (lastNotificationTimeMillis == 0L) return false
        return (currentTimeMillis - lastNotificationTimeMillis) < cooldownIntervalMillis
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun sendNotification(notificationId: Int, title: String, message: String): Boolean {
        if (!hasPostNotificationPermission()) {
            return false
        }
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_air_quality)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        return try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build())
                true
            } else {
                false
            }
        } catch (_: SecurityException) {
            false
        }
    }

    fun sendTestNotification(): Boolean {
        return sendNotification(
            NOTIFICATION_ID_TEST,
            "Prueba de Notificación AirSense Chile",
            "El sistema de monitoreo de calidad del aire y normativa laboral está activo."
        )
    }

    fun sendAirQualityAlertIfNeeded(metrics: AirQualityMetrics, userNotificationsEnabled: Boolean): Boolean {
        return sendFriendlyThresholdAlertIfNeeded(
            metrics = metrics,
            userNotificationsEnabled = userNotificationsEnabled,
            forceIgnoreCooldown = false
        )
    }

    fun sendFriendlyThresholdAlertIfNeeded(
        metrics: AirQualityMetrics,
        userNotificationsEnabled: Boolean,
        currentTimeMillis: Long = System.currentTimeMillis(),
        forceIgnoreCooldown: Boolean = false
    ): Boolean {
        if (!shouldSendAlert(metrics, userNotificationsEnabled)) {
            return false
        }

        val currentLevel = getAlertLevel(metrics.aqi)
        val isWorseLevel = currentLevel.ordinal > lastNotifiedLevel.ordinal

        if (!forceIgnoreCooldown && isCooldownActive(currentTimeMillis) && !isWorseLevel) {
            return false
        }

        val title = createFriendlyNotificationTitle(metrics)
        val body = createFriendlyNotificationBody(metrics)

        val sent = sendNotification(NOTIFICATION_ID_ALERT, title, body)
        if (sent) {
            lastNotificationTimeMillis = currentTimeMillis
            lastNotifiedLevel = currentLevel
        }
        return sent
    }

    suspend fun sendAlertWithPersistentCooldown(
        metrics: AirQualityMetrics,
        userPrefs: UserPreferencesManager,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val isEnabled = userPrefs.isNotificationsEnabledFlow.first()
        if (!shouldSendAlert(metrics, isEnabled)) {
            return false
        }

        val lastTimestamp = userPrefs.lastNotifiedTimestampFlow.first()
        val lastLevelName = userPrefs.lastNotifiedLevelFlow.first()
        val lastLevel = try {
            AlertLevel.valueOf(lastLevelName)
        } catch (_: Exception) {
            AlertLevel.GOOD
        }

        val currentLevel = getAlertLevel(metrics.aqi)
        val isCooldownActive = (currentTimeMillis - lastTimestamp) < cooldownIntervalMillis
        val isWorseLevel = currentLevel.ordinal > lastLevel.ordinal

        if (isCooldownActive && !isWorseLevel) {
            return false
        }

        val title = createFriendlyNotificationTitle(metrics)
        val body = createFriendlyNotificationBody(metrics)

        val sent = sendNotification(NOTIFICATION_ID_ALERT, title, body)
        if (sent) {
            lastNotificationTimeMillis = currentTimeMillis
            lastNotifiedLevel = currentLevel
            userPrefs.saveLastNotificationState(currentTimeMillis, currentLevel.name)
        }
        return sent
    }
}
