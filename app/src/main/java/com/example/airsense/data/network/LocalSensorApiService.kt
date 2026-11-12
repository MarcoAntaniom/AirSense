package com.example.airsense.data.network

/**
 * @deprecated La red local WeMos D1 (AirSense_AP) ha sido reemplazada por conexión directa
 * con Google Firebase Realtime Database vía Internet (FirebaseSensorApiService).
 */
@Deprecated("Usar FirebaseSensorResponse directamente")
typealias LocalSensorResponse = FirebaseSensorResponse
