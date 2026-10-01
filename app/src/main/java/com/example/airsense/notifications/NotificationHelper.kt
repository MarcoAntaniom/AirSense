package com.example.airsense.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
 * Permite configurar canales de notificación, enviar alertas de calidad de aire
 * y realizar pruebas del módulo de notificaciones.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "airsense_alerts_channel"
        const val CHANNEL_NAME = "Alertas de Calidad del Aire"
        const val CHANNEL_DESCRIPTION = "Notificaciones sobre la calidad del aire y niveles de alerta."
        const val NOTIFICATION_ID_ALERT = 1001
        const val NOTIFICATION_ID_TEST = 1002

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
         * Evalúa si debe emitirse una alerta considerando la configuración de usuario y las métricas.
         */
        fun shouldSendAlert(metrics: AirQualityMetrics, userNotificationsEnabled: Boolean): Boolean {
            if (!userNotificationsEnabled) return false
            return (metrics.aqi > 50) || (metrics.co2Ppm > 1000) || (metrics.pm25Ug > 35)
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
     * Construye y publica una notificación.
     */
    fun sendNotification(notificationId: Int, title: String, message: String): Boolean {
        createNotificationChannel()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_air_quality)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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
        if (!shouldSendAlert(metrics, userNotificationsEnabled)) {
            return false
        }
        val level = getAlertLevel(metrics.aqi)
        val title = createNotificationTitle(level)
        val body = createNotificationBody(metrics)
        return sendNotification(NOTIFICATION_ID_ALERT, title, body)
    }
}
