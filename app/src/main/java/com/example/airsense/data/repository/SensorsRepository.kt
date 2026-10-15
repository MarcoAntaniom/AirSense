package com.example.airsense.data.repository

import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.SensorDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Repositorio encargado de centralizar y sanitizar el listado de dispositivos IoT (SensorDevice)
 * expuestos de forma reactiva mediante StateFlow.
 */
class SensorsRepository {

    private val _sensors = MutableStateFlow<List<SensorDevice>>(emptyList())
    val sensors: Flow<List<SensorDevice>> = _sensors.asStateFlow()

    fun setSensors(list: List<SensorDevice>) {
        _sensors.value = list.map { sanitizeSensor(it) }
    }

    fun updateSensor(updatedDevice: SensorDevice) {
        val sanitized = sanitizeSensor(updatedDevice)
        _sensors.update { currentList ->
            val index = currentList.indexOfFirst { it.id == sanitized.id }
            if (index != -1) {
                currentList.toMutableList().apply { set(index, sanitized) }
            } else {
                currentList + sanitized
            }
        }
    }

    /**
     * Sanitiza y valida las lecturas de un dispositivo IoT.
     * Si las lecturas contienen valores fuera de rango físico, corruptos o NaN,
     * se marca como offline o error.
     */
    fun sanitizeSensor(device: SensorDevice): SensorDevice {
        val reading = device.lastReading
        val isCorruptReading = reading.contains("NaN", ignoreCase = true) ||
                reading.contains("null", ignoreCase = true)

        val isBatteryValid = device.batteryLevel in 0..100

        if (!isBatteryValid || isCorruptReading) {
            return device.copy(
                isOnline = false,
                lastReading = if (isCorruptReading) "Error Lectura (NaN)" else "Error Batería"
            )
        }

        return device
    }

    /**
     * Sanitiza una instancia de AirQualityMetrics asegurando que sus valores se mantengan
     * dentro de rangos físicos coherentes.
     */
    fun sanitizeMetrics(metrics: AirQualityMetrics): AirQualityMetrics {
        val safeAqi = metrics.aqi.coerceAtLeast(0)
        val safeTemp = if (metrics.temperatureCelsius.isNaN() || metrics.temperatureCelsius < -50.0 || metrics.temperatureCelsius > 100.0) {
            22.5
        } else metrics.temperatureCelsius
        val safeHumidity = metrics.humidityPercentage.coerceIn(0, 100)
        val safeCo2 = metrics.co2Ppm.coerceAtLeast(0)
        val safePm25 = metrics.pm25Ug.coerceAtLeast(0)

        return metrics.copy(
            aqi = safeAqi,
            temperatureCelsius = safeTemp,
            humidityPercentage = safeHumidity,
            co2Ppm = safeCo2,
            pm25Ug = safePm25
        )
    }

    /**
     * Genera la lista estandarizada de dispositivos a partir de las métricas recibidas.
     */
    fun generateSensorsFromMetrics(metrics: AirQualityMetrics, isOnline: Boolean): List<SensorDevice> {
        val sanitizedMetrics = sanitizeMetrics(metrics)
        val list = listOf(
            SensorDevice(
                id = "sns_01",
                name = "Estación Clima Principal",
                type = "AQI + Temp",
                batteryLevel = 95,
                isOnline = isOnline,
                lastReading = "${sanitizedMetrics.aqi} AQI"
            ),
            SensorDevice(
                id = "sns_02",
                name = "Sensor Temperatura (DHT11)",
                type = "Temp + Humedad",
                batteryLevel = 88,
                isOnline = isOnline,
                lastReading = "${sanitizedMetrics.temperatureCelsius} °C"
            ),
            SensorDevice(
                id = "sns_03",
                name = "Sensor Calidad Aire (MQ)",
                type = "Gas MQ + PM2.5",
                batteryLevel = 90,
                isOnline = isOnline,
                lastReading = "${sanitizedMetrics.co2Ppm} ppm"
            ),
            SensorDevice(
                id = "sns_04",
                name = "Sensor Humedad Amb.",
                type = "Humedad Relativa",
                batteryLevel = 82,
                isOnline = isOnline,
                lastReading = "${sanitizedMetrics.humidityPercentage}%"
            )
        )
        setSensors(list)
        return _sensors.value
    }
}
