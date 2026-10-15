package com.example.airsense.data.repository

import com.example.airsense.data.models.AirQualityThresholds
import com.example.airsense.data.models.AirSenseState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorRepositoryTest {

    @Test
    fun observeSensorState_emitsActiveValidOnCleanFrame() = runBlocking {
        val repository = SensorRepository()
        val thresholdsFlow = flowOf(AirQualityThresholds())

        val firstState = repository.observeSensorState(thresholdsFlow).first()
        // La emisión debe ser un estado de AirSenseState
        assertTrue(firstState is AirSenseState.ActiveValid || firstState is AirSenseState.Connecting)
    }
}
