package com.example.airsense.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ChecksumValidatorTest {

    @Test
    fun parseAndValidateFrame_validFrame_returnsParsedData() {
        // Trama válida: "24.5,55,180,OK" -> XOR Checksum
        val payload = "24.5,55,180,OK"
        var xorSum = 0
        for (c in payload) {
            xorSum = xorSum xor c.code
        }
        val hexChecksum = String.format("%02X", xorSum)
        val rawFrame = "$payload*$hexChecksum\n"

        val result = ChecksumValidator.parseAndValidateFrame(rawFrame)

        assertNotNull(result)
        assertEquals(24.5, result!!.temperatureCelsius, 0.01)
        assertEquals(55, result.humidityPercentage)
        assertEquals(180, result.co2Ppm)
        assertEquals("OK", result.statusText)
    }

    @Test
    fun parseAndValidateFrame_invalidChecksum_returnsNull() {
        val rawFrame = "24.5,55,180,OK*FF\n"
        val result = ChecksumValidator.parseAndValidateFrame(rawFrame)
        assertNull(result)
    }

    @Test
    fun parseAndValidateFrame_truncatedFrame_returnsNull() {
        val rawFrame = "24.5,55"
        val result = ChecksumValidator.parseAndValidateFrame(rawFrame)
        assertNull(result)
    }

    @Test
    fun parseAndValidateFrame_outOfRangeDht11Temp_returnsNull() {
        // Temperatura 85°C supera el rango de 50°C del DHT11
        val payload = "85.0,55,180,OK"
        var xorSum = 0
        for (c in payload) {
            xorSum = xorSum xor c.code
        }
        val hexChecksum = String.format("%02X", xorSum)
        val rawFrame = "$payload*$hexChecksum\n"

        val result = ChecksumValidator.parseAndValidateFrame(rawFrame)
        assertNull(result)
    }
}
