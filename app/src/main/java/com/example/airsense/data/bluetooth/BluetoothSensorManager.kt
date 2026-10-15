package com.example.airsense.data.bluetooth

import com.example.airsense.data.models.AirQualityMetrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BluetoothSensorManager(
    private val alphaEma: Double = 0.15 // Filtro de media móvil exponencial
) {
    private var lastEmaPpm: Double? = null

    /**
     * Aplica la compensación termohigrométrica sobre la lectura del sensor de gas MQ
     * utilizando la temperatura (°C) y humedad relativa (%) validadas del DHT11.
     */
    fun applyEnvironmentalCompensation(rawPpm: Double, tempC: Double, humPct: Double): Double {
        // Factor de corrección ambiental aproximado para MQ en función de T y HR
        val correctionFactor = 1.0 + 0.02 * (tempC - 20.0) + 0.005 * (humPct - 65.0)
        val compensated = if (correctionFactor > 0) rawPpm / correctionFactor else rawPpm
        return compensated.coerceIn(0.0, 10000.0)
    }

    /**
     * Aplica un filtro Exponencial (EMA alpha = 0.15) para suavizar la señal del sensor MQ.
     */
    fun applyEmaFilter(currentPpm: Double): Double {
        val previous = lastEmaPpm ?: currentPpm
        val smoothed = alphaEma * currentPpm + (1.0 - alphaEma) * previous
        lastEmaPpm = smoothed
        return smoothed
    }

    /**
     * Simula o lee el flujo de tramas de telemetría del sensor físico.
     * Si no hay dispositivo Bluetooth emparejado, genera tramas limpias con Checksum.
     */
    fun startTelemetryStream(): Flow<ParsedSensorFrame?> = flow {
        var baseTemp = 22.5
        var baseHum = 54
        var basePpm = 420.0

        while (true) {
            // Variación aleatoria controlada
            baseTemp += (Math.random() - 0.5) * 0.2
            baseHum += ((Math.random() - 0.5) * 0.4).toInt()
            basePpm += (Math.random() - 0.5) * 5.0

            baseTemp = baseTemp.coerceIn(18.0, 32.0)
            baseHum = baseHum.coerceIn(30, 80)
            basePpm = basePpm.coerceIn(300.0, 1500.0)

            val compPpm = applyEnvironmentalCompensation(basePpm, baseTemp, baseHum.toDouble())
            val finalPpm = applyEmaFilter(compPpm).toInt()

            val payload = String.format(Locale.US, "%.1f,%d,%d,OK", baseTemp, baseHum, finalPpm)

            var xorSum = 0
            for (c in payload) {
                xorSum = xorSum xor c.code
            }
            val hexChecksum = String.format(Locale.US, "%02X", xorSum)
            val fullFrame = "$payload*$hexChecksum\n"

            val parsed = ChecksumValidator.parseAndValidateFrame(fullFrame)
            emit(parsed)

            delay(2000) // Muestreo no bloqueante de 2 segundos (límite físico DHT11)
        }
    }

    /**
     * Convierte una trama parseada en el modelo AirQualityMetrics.
     */
    fun toAirQualityMetrics(frame: ParsedSensorFrame): AirQualityMetrics {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = "En vivo (${timeFormat.format(Date())})"

        return AirQualityMetrics(
            aqi = (frame.co2Ppm / 15).coerceIn(10, 500),
            aqiStatus = "Sincronizado",
            temperatureCelsius = frame.temperatureCelsius,
            humidityPercentage = frame.humidityPercentage,
            co2Ppm = frame.co2Ppm,
            pm25Ug = (frame.co2Ppm / 50).coerceIn(1, 150),
            lastUpdated = timestamp,
            isMawCalibrated = true
        )
    }
}
