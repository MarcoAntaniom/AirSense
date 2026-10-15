package com.example.airsense.data.bluetooth

import java.util.Locale

data class ParsedSensorFrame(
    val temperatureCelsius: Double,
    val humidityPercentage: Int,
    val co2Ppm: Int,
    val statusText: String,
    val isValid: Boolean
)

object ChecksumValidator {

    /**
     * Valida y parsea una trama con formato "T,H,PPM,EST*CS\n" (ej: "24.5,55,180,OK*4A").
     * Calcula la suma XOR hexadecimal del contenido anterior al carácter '*'.
     */
    fun parseAndValidateFrame(rawFrame: String): ParsedSensorFrame? {
        val trimmed = rawFrame.trim()
        if (trimmed.isEmpty()) return null

        val asteriskIndex = trimmed.lastIndexOf('*')
        if (asteriskIndex == -1 || asteriskIndex + 1 >= trimmed.length) return null

        val payload = trimmed.substring(0, asteriskIndex)
        val expectedChecksumHex = trimmed.substring(asteriskIndex + 1).trim()

        // Calcular XOR Checksum
        var calculatedXor = 0
        for (char in payload) {
            calculatedXor = calculatedXor xor char.code
        }

        val calculatedHex = String.format(Locale.US, "%02X", calculatedXor)
        if (!calculatedHex.equals(expectedChecksumHex, ignoreCase = true)) {
            return null // Checksum inválido
        }

        // Separar valores numéricos
        val parts = payload.split(",")
        if (parts.size < 4) return null

        return try {
            val temp = parts[0].toDouble()
            val humidity = parts[1].toDouble().toInt()
            val ppm = parts[2].toDouble().toInt()
            val status = parts[3].trim()

            // Validar límites según hoja de datos oficial del DHT11 y rango ADC seguro
            val validTemp = temp in 0.0..50.0
            val validHumidity = humidity in 20..90
            val validPpm = ppm in 0..10000

            if (validTemp && validHumidity && validPpm) {
                ParsedSensorFrame(
                    temperatureCelsius = temp,
                    humidityPercentage = humidity,
                    co2Ppm = ppm,
                    statusText = status,
                    isValid = true
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
