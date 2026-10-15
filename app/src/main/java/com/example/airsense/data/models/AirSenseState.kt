package com.example.airsense.data.models

enum class AirQualityLevel(val label: String) {
    EXCELLENT("Excelente"),
    MODERATE("Moderada"),
    UNHEALTHY_SENSITIVE("Dañina para grupos sensibles"),
    UNHEALTHY("Mala"),
    HAZARDOUS("Peligrosa")
}

/**
 * Máquina de estados finita de la interfaz de usuario en AirSense v1.1.1.
 */
sealed class AirSenseState {
    object Connecting : AirSenseState()

    data class WarmingUp(val remainingSeconds: Int) : AirSenseState()

    data class ActiveValid(
        val metrics: AirQualityMetrics,
        val level: AirQualityLevel,
        val isOffline: Boolean = false
    ) : AirSenseState()

    data class Stale(
        val lastValidMetrics: AirQualityMetrics,
        val reason: String
    ) : AirSenseState()
}
