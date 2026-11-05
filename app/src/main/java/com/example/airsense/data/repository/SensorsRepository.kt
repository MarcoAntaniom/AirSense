package com.example.airsense.data.repository

import com.example.airsense.data.models.AirQualityMetrics
import com.example.airsense.data.models.SensorDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

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
        val safeTemp = if (metrics.temperature.isNaN() || metrics.temperature < -50.0f || metrics.temperature > 100.0f) {
            22.4f
        } else metrics.temperature
        val safeHumidity = metrics.humidity.coerceIn(0.0f, 100.0f)
        val safeCo2 = metrics.co2.coerceAtLeast(0)

        return metrics.copy(
            aqi = safeAqi,
            temperature = safeTemp,
            humidity = safeHumidity,
            co2 = safeCo2,
            co = metrics.co.coerceAtLeast(0f),
            ch2o = metrics.ch2o.coerceAtLeast(0f),
            pm25 = metrics.pm25.coerceAtLeast(0f),
            pm10 = metrics.pm10.coerceAtLeast(0f),
            so2 = metrics.so2.coerceAtLeast(0f)
        )
    }

    /**
     * Genera la lista estandarizada de dispositivos a partir de las métricas recibidas de la placa WeMos D1.
     */
    fun generateSensorsFromMetrics(metrics: AirQualityMetrics, isOnline: Boolean): List<SensorDevice> {
        val sanitized = sanitizeMetrics(metrics)
        val tempStr = String.format(Locale.US, "%.1f °C", sanitized.temperature)
        val coStr = String.format(Locale.US, "%.1f ppm", sanitized.co)
        val ch2oStr = String.format(Locale.US, "%.2f ppm", sanitized.ch2o)
        val pm25Str = String.format(Locale.US, "%.1f µg/m³", sanitized.pm25)
        val pm10Str = String.format(Locale.US, "%.1f µg/m³", sanitized.pm10)
        val so2Str = String.format(Locale.US, "%.1f µg/m³", sanitized.so2)

        val list = listOf(
            SensorDevice(
                id = "sns_01",
                name = "Estación Clima (WeMos D1)",
                type = "AQI + Temp",
                batteryLevel = 95,
                isOnline = isOnline,
                lastReading = "${sanitized.aqi} AQI"
            ),
            SensorDevice(
                id = "sns_02",
                name = "Sensor Monóxido de Carbono (CO)",
                type = "Gas CO (LPP 40 ppm)",
                batteryLevel = 90,
                isOnline = isOnline,
                lastReading = coStr
            ),
            SensorDevice(
                id = "sns_03",
                name = "Sensor Formaldehído (CH₂O)",
                type = "Gas CH₂O (LPA 0.3 ppm)",
                batteryLevel = 88,
                isOnline = isOnline,
                lastReading = ch2oStr
            ),
            SensorDevice(
                id = "sns_04",
                name = "Sensor CO₂ / Ventilación",
                type = "Dióxido de Carbono",
                batteryLevel = 92,
                isOnline = isOnline,
                lastReading = "${sanitized.co2} ppm"
            ),
            SensorDevice(
                id = "sns_05",
                name = "Sensor Temperatura (TGBH)",
                type = "DHT11 (Max 26.7 °C)",
                batteryLevel = 85,
                isOnline = isOnline,
                lastReading = tempStr
            ),
            SensorDevice(
                id = "sns_06",
                name = "Sensor Humedad Relativa",
                type = "Humedad DHT11",
                batteryLevel = 82,
                isOnline = isOnline,
                lastReading = "${sanitized.humidity.toInt()}%"
            ),
            SensorDevice(
                id = "sns_07",
                name = "Calidad Exterior (PM2.5 / PM10)",
                type = "Norma MMA Chile",
                batteryLevel = 99,
                isOnline = isOnline,
                lastReading = "PM2.5: $pm25Str | PM10: $pm10Str | SO2: $so2Str"
            )
        )
        setSensors(list)
        return _sensors.value
    }
}
