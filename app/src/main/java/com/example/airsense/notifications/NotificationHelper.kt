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
import kotlinx.coroutines.flow.first

enum class AlertLevel {
    GOOD,
    MODERATE,
    UNHEALTHY,
    HAZARDOUS
}

/**
 * Módulo de gestión de notificaciones para la aplicación AirSense.
 * Permite configurar canales de notificación, enviar alertas amigables por umbrales
 * (temperatura, humedad, calidad del aire/gas) evitando spam mediante un intervalo de enfriamiento (cooldown)
 * y persistencia de nivel de severidad.
 */
class NotificationHelper(private val context: Context) {

    var cooldownIntervalMillis: Long = DEFAULT_COOLDOWN_MILLIS
    var lastNotificationTimeMillis: Long = 0L
    var lastNotifiedLevel: AlertLevel = AlertLevel.GOOD

    companion object {
        const val CHANNEL_ID = "airsense_alerts_channel"
        const val CHANNEL_NAME = "Alertas de Calidad del Aire"
        const val CHANNEL_DESCRIPTION = "Notificaciones sobre la calidad del aire y niveles de alerta."
        const val NOTIFICATION_ID_ALERT = 1001
        const val NOTIFICATION_ID_TEST = 1002

        // Umbrales normales de confort ambiental
        const val TEMP_MIN_NORMAL = 18.0
        const val TEMP_MAX_NORMAL = 26.0
        const val HUMIDITY_MIN_NORMAL = 30
        const val HUMIDITY_MAX_NORMAL = 60
        const val AQI_THRESHOLD = 50
        const val CO2_THRESHOLD = 1000
        const val PM25_THRESHOLD = 35

        // Cooldown por defecto: 15 minutos
        const val DEFAULT_COOLDOWN_MILLIS = 15 * 60 * 1000L

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

            val isAirQualityElevated = (metrics.aqi > AQI_THRESHOLD) ||
                    (metrics.co2Ppm > CO2_THRESHOLD) ||
                    (metrics.pm25Ug > PM25_THRESHOLD)

            val isTempOutOfBounds = (metrics.temperatureCelsius < TEMP_MIN_NORMAL) ||
                    (metrics.temperatureCelsius > TEMP_MAX_NORMAL)

            val isHumidityOutOfBounds = (metrics.humidityPercentage < HUMIDITY_MIN_NORMAL) ||
                    (metrics.humidityPercentage > HUMIDITY_MAX_NORMAL)

            return isAirQualityElevated || isTempOutOfBounds || isHumidityOutOfBounds
        }

        fun createNotificationTitle(alertLevel: AlertLevel): String {
            return when (alertLevel) {
                AlertLevel.GOOD -> "Calidad del aire óptima"
                AlertLevel.MODERATE -> "Aviso: Calidad del aire moderada"
                AlertLevel.UNHEALTHY -> "Alerta: Calidad del aire no saludable"
                AlertLevel.HAZARDOUS -> "Peligro: Calidad del aire crítica"
            }
        }

        fun createNotificationBody(metrics: AirQualityMetrics): String {
            return "AQI: ${metrics.aqi} (${metrics.aqiStatus}) • CO2: ${metrics.co2Ppm} ppm • PM2.5: ${metrics.pm25Ug} µg/m³"
        }

        fun createFriendlyNotificationTitle(metrics: AirQualityMetrics): String {
            return "Consejo de Confort AirSense"
        }

        fun createFriendlyNotificationBody(metrics: AirQualityMetrics): String {
            val messages = mutableListOf<String>()

            if (metrics.humidityPercentage < HUMIDITY_MIN_NORMAL) {
                messages.add("Parece que el ambiente está un poco seco (${metrics.humidityPercentage}%). Considera ventilar la habitación o beber agua.")
            } else if (metrics.humidityPercentage > HUMIDITY_MAX_NORMAL) {
                messages.add("La humedad está algo elevada (${metrics.humidityPercentage}%). Te recomendamos ventilar la habitación para renovar el aire.")
            }

            if (metrics.temperatureCelsius < TEMP_MIN_NORMAL) {
                messages.add("La temperatura ha bajado (${metrics.temperatureCelsius}°C). Abrígate bien o ajusta la calefacción.")
            } else if (metrics.temperatureCelsius > TEMP_MAX_NORMAL) {
                messages.add("Hace un poco de calor (${metrics.temperatureCelsius}°C). Mantén la habitación ventilada y bebe agua fresca.")
            }

            if (metrics.aqi > AQI_THRESHOLD || metrics.co2Ppm > CO2_THRESHOLD || metrics.pm25Ug > PM25_THRESHOLD) {
                messages.add("La calidad del aire no es la ideal (AQI: ${metrics.aqi}). Procura abrir ventanas para renovar el flujo de aire.")
            }

            return if (messages.isNotEmpty()) {
                messages.joinToString(" ")
            } else {
                createNotificationBody(metrics)
            }
        }
    }

    /**
     * Verifica la validez del permiso POST_NOTIFICATIONS en Android 13+ (API 33+).
     */
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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
            "Prueba de Notificación AirSense",
            "El módulo de notificaciones de AirSense está funcionando correctamente."
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
