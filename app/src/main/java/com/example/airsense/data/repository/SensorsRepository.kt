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
                name = "Calidad del Aire (AQI)",
                type = "Índice General",
                batteryLevel = 98,
                isOnline = isOnline,
                lastReading = "${sanitized.aqi} AQI",
                referenceRange = "0 - 50 (Excelente) · EPA",
                description = "Índice ponderado en tiempo real según normativas ambientales. Evalúa la calidad global del aire que respiras."
            ),
            SensorDevice(
                id = "sns_02",
                name = "Monóxido de Carbono",
                type = "Gas CO · Combustión",
                batteryLevel = 94,
                isOnline = isOnline,
                lastReading = coStr,
                referenceRange = "< 40 ppm (DS 594 Chile)",
                description = "Gas incoloro e inodoro emitido por mala combustión en calefactores o estufas. El límite ponderado seguro es 40 ppm."
            ),
            SensorDevice(
                id = "sns_03",
                name = "Formaldehído (CH₂O)",
                type = "Compuesto Volátil (COV)",
                batteryLevel = 92,
                isOnline = isOnline,
                lastReading = ch2oStr,
                referenceRange = "< 0.30 ppm (LPA DS 594)",
                description = "Vapor químico desprendido por barnices, resinas, muebles aglomerados y pegamentos. Límite máximo 0.3 ppm."
            ),
            SensorDevice(
                id = "sns_04",
                name = "Dióxido de Carbono (CO₂)",
                type = "Ventilación Interior",
                batteryLevel = 96,
                isOnline = isOnline,
                lastReading = "${sanitized.co2} ppm",
                referenceRange = "< 700 ppm (Óptimo) · ASHRAE",
                description = "Indicador directo de ventilación y oxígeno ambiental. Concentraciones > 1000 ppm generan somnolencia y fatiga."
            ),
            SensorDevice(
                id = "sns_05",
                name = "Temperatura Ambiente",
                type = "Sensor DHT11",
                batteryLevel = 90,
                isOnline = isOnline,
                lastReading = tempStr,
                referenceRange = "18.0 °C - 24.0 °C (Confort)",
                description = "Temperatura interior para evitar estrés térmico o irritaciones respiratorias. Rango ideal entre 18 y 24 °C."
            ),
            SensorDevice(
                id = "sns_06",
                name = "Humedad Relativa",
                type = "Sensor DHT11",
                batteryLevel = 88,
                isOnline = isOnline,
                lastReading = "${sanitized.humidity.toInt()}%",
                referenceRange = "30% - 60% HR (Saludable)",
                description = "Humedad relativa en el ambiente. Menor al 30% reseca mucosas; sobre el 60% fomenta la aparición de ácaros y moho."
            ),
            SensorDevice(
                id = "sns_07",
                name = "Material Particulado",
                type = "Partículas Finas PM2.5",
                batteryLevel = 99,
                isOnline = isOnline,
                lastReading = pm25Str,
                referenceRange = "< 25.0 µg/m³ · MMA Chile",
                description = "Partículas finas microscópicas capaces de ingresar al sistema respiratorio profundo. PM10: $pm10Str | SO2: $so2Str"
            )
        )
        setSensors(list)
        return _sensors.value
    }
}
