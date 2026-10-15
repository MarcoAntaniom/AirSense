package com.example.airsense.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.airsense.MainActivity
import com.example.airsense.R
import com.example.airsense.data.UserPreferencesManager
import com.example.airsense.data.models.AirQualityLevel
import com.example.airsense.data.models.AirSenseState
import com.example.airsense.data.repository.SensorRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SensorMonitoringService : Service() {

    companion object {
        const val CHANNEL_ID = "airsense_monitoring_service_channel"
        const val CHANNEL_NAME = "Monitoreo en Segundo Plano"
        const val SERVICE_NOTIFICATION_ID = 2001

        const val ACTION_START = "com.example.airsense.ACTION_START_MONITORING"
        const val ACTION_STOP = "com.example.airsense.ACTION_STOP_MONITORING"

        fun startService(context: Context) {
            val intent = Intent(context, SensorMonitoringService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SensorMonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var sensorRepository: SensorRepository
    private lateinit var userPrefs: UserPreferencesManager
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        sensorRepository = SensorRepository()
        userPrefs = UserPreferencesManager(this)
        notificationHelper = NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(SERVICE_NOTIFICATION_ID, buildForegroundNotification("Inicializando monitoreo..."))

        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            sensorRepository.observeSensorState(userPrefs.thresholdsFlow).collect { state ->
                when (state) {
                    is AirSenseState.ActiveValid -> {
                        val notifText = "Gas: ${state.metrics.co2Ppm} PPM • Status: ${state.level.label}"
                        updateForegroundNotification(notifText)

                        if (state.level == AirQualityLevel.UNHEALTHY || state.level == AirQualityLevel.HAZARDOUS) {
                            val notifEnabled = userPrefs.isNotificationsEnabledFlow.first()
                            notificationHelper.sendFriendlyThresholdAlertIfNeeded(
                                metrics = state.metrics,
                                userNotificationsEnabled = notifEnabled
                            )
                        }
                    }
                    is AirSenseState.Stale -> {
                        updateForegroundNotification("Aviso: Sensor desconectado o reconectando...")
                    }
                    is AirSenseState.WarmingUp -> {
                        updateForegroundNotification("Sensor MQ en precalentamiento (${state.remainingSeconds}s)")
                    }
                    is AirSenseState.Connecting -> {
                        updateForegroundNotification("Conectando con estación de sensores...")
                    }
                }
            }
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_air_quality)
            .setContentTitle("AirSense - Monitoreo Activo")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateForegroundNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(SERVICE_NOTIFICATION_ID, buildForegroundNotification(contentText))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
