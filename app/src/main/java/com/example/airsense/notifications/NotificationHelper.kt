package com.example.airsense.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.airsense.MainActivity
import com.example.airsense.R
import com.example.airsense.data.models.AirQualityMetrics

enum class AlertLevel {
    GOOD,
    MODERATE,
    UNHEALTHY,
    HAZARDOUS
}

/**
 * Módulo de gestión de notificaciones para la aplicación AirSense.
 * Permite configurar canales de notificación, enviar alertas amigables por umbrales
 * (temperatura, humedad, calidad del aire/gas) evitando spam mediante un intervalo de enfriamiento (cooldown).
 */
class NotificationHelper(private val context: Context) {

    // Manejo de estado de enfriamiento (cooldown) para evitar spam de alertas
    var cooldownIntervalMillis: Long = DEFAULT_COOLDOWN_MILLIS
    var lastNotificationTimeMillis: Long = 0L

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

        /**
         * Determina el nivel de alerta según el valor del índice AQI.
         */
        fun getAlertLevel(aqi: Int): AlertLevel {
            return when {
                aqi <= 50 -> AlertLevel.GOOD
                aqi <= 100 -> AlertLevel.MODERATE
                aqi <= 200 -> AlertLevel.UNHEALTHY
                else -> AlertLevel.HAZARDOUS
            }
        }

        /**
         * Evalúa si debe emitirse una alerta considerando la configuración de usuario y las métricas
         * (AQI, CO2, PM2.5, así como temperatura y humedad fuera de rangos normales por encima o debajo).
         */
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

        /**
         * Genera un título descriptivo para la notificación de alerta.
         */
        fun createNotificationTitle(alertLevel: AlertLevel): String {
            return when (alertLevel) {
                AlertLevel.GOOD -> "Calidad del aire óptima"
                AlertLevel.MODERATE -> "Aviso: Calidad del aire moderada"
                AlertLevel.UNHEALTHY -> "Alerta: Calidad del aire no saludable"
                AlertLevel.HAZARDOUS -> "Peligro: Calidad del aire crítica"
            }
        }

        /**
         * Formatea el cuerpo del mensaje de alerta según las métricas.
         */
        fun createNotificationBody(metrics: AirQualityMetrics): String {
            return "AQI: ${metrics.aqi} (${metrics.aqiStatus}) • CO2: ${metrics.co2Ppm} ppm • PM2.5: ${metrics.pm25Ug} µg/m³"
        }

        /**
         * Genera un título amigable y empático para notificaciones de confort.
         */
        fun createFriendlyNotificationTitle(metrics: AirQualityMetrics): String {
            return "Consejo de Confort AirSense"
        }

        /**
         * Genera mensajes amigables, empáticos y orientados a la acción cuando las métricas
         * están fuera de rangos normales (tanto por encima como por debajo).
         */
        fun createFriendlyNotificationBody(metrics: AirQualityMetrics): String {
            val messages = mutableListOf<String>()

            // Evaluación de Humedad
            if (metrics.humidityPercentage < HUMIDITY_MIN_NORMAL) {
                messages.add("Parece que el ambiente está un poco seco (${metrics.humidityPercentage}%). Considera ventilar la habitación o beber agua.")
            } else if (metrics.humidityPercentage > HUMIDITY_MAX_NORMAL) {
                messages.add("La humedad está algo elevada (${metrics.humidityPercentage}%). Te recomendamos ventilar la habitación para renovar el aire.")
            }

            // Evaluación de Temperatura
            if (metrics.temperatureCelsius < TEMP_MIN_NORMAL) {
                messages.add("La temperatura ha bajado (${metrics.temperatureCelsius}°C). Abrígate bien o ajusta la calefacción.")
            } else if (metrics.temperatureCelsius > TEMP_MAX_NORMAL) {
                messages.add("Hace un poco de calor (${metrics.temperatureCelsius}°C). Mantén la habitación ventilada y bebe agua fresca.")
            }

            // Evaluación de Calidad de Aire / CO2 / PM2.5
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
     * Verifica si el intervalo de enfriamiento (cooldown) se encuentra activo.
     */
    fun isCooldownActive(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (lastNotificationTimeMillis == 0L) return false
        return (currentTimeMillis - lastNotificationTimeMillis) < cooldownIntervalMillis
    }

    /**
     * Crea el canal de notificación en dispositivos con Android 8.0 (API 26) o superior.
     */
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

    /**
     * Construye y publica una notificación garantizando compatibilidad con Android 13+.
     */
    fun sendNotification(notificationId: Int, title: String, message: String): Boolean {
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

    /**
     * Envía una notificación de prueba para verificar el funcionamiento del módulo.
     */
    fun sendTestNotification(): Boolean {
        return sendNotification(
            NOTIFICATION_ID_TEST,
            "Prueba de Notificación AirSense",
            "El módulo de notificaciones de AirSense está funcionando correctamente."
        )
    }

    /**
     * Envía una alerta si la métrica de calidad de aire excede los umbrales configurados.
     */
    fun sendAirQualityAlertIfNeeded(metrics: AirQualityMetrics, userNotificationsEnabled: Boolean): Boolean {
        return sendFriendlyThresholdAlertIfNeeded(
            metrics = metrics,
            userNotificationsEnabled = userNotificationsEnabled,
            forceIgnoreCooldown = true
        )
    }

    /**
     * Envía una notificación de alerta amigable por umbrales si las métricas están fuera de rango,
     * respetando el intervalo de enfriamiento (cooldown) para evitar spam de alertas.
     */
    fun sendFriendlyThresholdAlertIfNeeded(
        metrics: AirQualityMetrics,
        userNotificationsEnabled: Boolean,
        currentTimeMillis: Long = System.currentTimeMillis(),
        forceIgnoreCooldown: Boolean = false
    ): Boolean {
        if (!shouldSendAlert(metrics, userNotificationsEnabled)) {
            return false
        }

        if (!forceIgnoreCooldown && isCooldownActive(currentTimeMillis)) {
            return false
        }

        val title = createFriendlyNotificationTitle(metrics)
        val body = createFriendlyNotificationBody(metrics)

        val sent = sendNotification(NOTIFICATION_ID_ALERT, title, body)
        if (sent) {
            lastNotificationTimeMillis = currentTimeMillis
        }
        return sent
    }
}
