package com.example.airsense.data.models

enum class AirQualityLevel(val label: String) {
    EXCELLENT("Excelente"),
    MODERATE("Moderada"),
    UNHEALTHY_SENSITIVE("Dañina para grupos sensibles"),
    UNHEALTHY("Mala"),
    HAZARDOUS("Peligrosa")
}

/**
 * Máquina de estados finita de la interfaz de usuario en AirSense.
 */
sealed class AirSenseState {
    object Connecting : AirSenseState()

    data class WarmingUp(val remainingSeconds: Int) : AirSenseState()

    data class ActiveValid(
        val metrics: AirQualityMetrics,
        val level: AirQualityLevel
    ) : AirSenseState()

    data class Stale(
        val lastValidMetrics: AirQualityMetrics,
        val reason: String
    ) : AirSenseState()
}
