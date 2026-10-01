package com.example.airsense.data.models

data class SensorDevice(
    val id: String,
    val name: String,
    val type: String,
    val batteryLevel: Int,
    val isOnline: Boolean,
    val lastReading: String
)