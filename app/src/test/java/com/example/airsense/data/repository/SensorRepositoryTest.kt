package com.example.airsense.data.repository

import com.example.airsense.data.models.AirQualityThresholds
import com.example.airsense.data.models.AirSenseState
import com.example.airsense.data.network.LocalSensorApiService
import com.example.airsense.data.network.LocalSensorResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorRepositoryTest {

    @Test
    fun observeSensorState_emitsActiveValidOnCleanFrame() = runBlocking {
        val fakeApiService = object : LocalSensorApiService {
            override suspend fun getSensorData(): LocalSensorResponse {
                return LocalSensorResponse(
                    temperature = 22.4f,
                    humidity = 55.0f,
                    gasRaw = 210,
                    co2 = 450,
                    aqi = 35,
                    timestamp = 15430L
                )
            }
        }

        val repository = SensorRepository(localSensorApiService = fakeApiService)
        val thresholdsFlow = flowOf(AirQualityThresholds())

        val firstState = repository.observeSensorState(thresholdsFlow).first()
        assertTrue(firstState is AirSenseState.ActiveValid)
    }

    @Test
    fun observeSensorState_emitsStaleOnNetworkFailure() = runBlocking {
        val failingApiService = object : LocalSensorApiService {
            override suspend fun getSensorData(): LocalSensorResponse {
                throw java.net.SocketTimeoutException("Connect timed out")
            }
        }

        val repository = SensorRepository(localSensorApiService = failingApiService)
        val thresholdsFlow = flowOf(AirQualityThresholds())

        val firstState = repository.observeSensorState(thresholdsFlow).first()
        assertTrue(firstState is AirSenseState.Stale)
    }
}
