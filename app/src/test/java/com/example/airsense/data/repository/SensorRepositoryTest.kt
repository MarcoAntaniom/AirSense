package com.example.airsense.data.repository

import com.example.airsense.data.models.AirQualityThresholds
import com.example.airsense.data.models.AirSenseState
import com.example.airsense.data.models.SensorConnectionSource
import com.example.airsense.data.network.FirebaseSensorApiService
import com.example.airsense.data.network.FirebaseSensorResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorRepositoryTest {

    @Test
    fun observeSensorState_emitsActiveValidOnFirebaseResponse() = runBlocking {
        val fakeFirebaseApi = object : FirebaseSensorApiService {
            override suspend fun getLiveMetrics(auth: String?): FirebaseSensorResponse {
                return FirebaseSensorResponse(
                    temperature = 22.4f,
                    humidity = 55.0f,
                    gasRaw = 210,
                    co2 = 450,
                    aqi = 35,
                    timestamp = 15430L,
                    epoch = 1731420000L
                )
            }

            override suspend fun getHistory(
                auth: String?,
                orderBy: String?,
                limitToLast: Int?
            ): Map<String, FirebaseSensorResponse>? = null
        }

        val repository = SensorRepository(firebaseSensorApiService = fakeFirebaseApi)
        val thresholdsFlow = flowOf(AirQualityThresholds())

        val firstState = repository.observeSensorState(thresholdsFlow).first()
        assertTrue(firstState is AirSenseState.ActiveValid)
        assertEquals(SensorConnectionSource.FIREBASE_CLOUD, repository.connectionSource.value)
        assertTrue(repository.isConnected.value)
        val activeMetrics = (firstState as AirSenseState.ActiveValid).metrics
        assertEquals(1731420000000L, activeMetrics.timestamp)
    }

    @Test
    fun observeSensorState_emitsStaleWhenFirebaseFails() = runBlocking {
        val failingFirebaseApi = object : FirebaseSensorApiService {
            override suspend fun getLiveMetrics(auth: String?): FirebaseSensorResponse? {
                throw java.net.UnknownHostException("No connection to Firebase")
            }

            override suspend fun getHistory(
                auth: String?,
                orderBy: String?,
                limitToLast: Int?
            ): Map<String, FirebaseSensorResponse>? = null
        }

        val repository = SensorRepository(firebaseSensorApiService = failingFirebaseApi)
        val thresholdsFlow = flowOf(AirQualityThresholds())

        val firstState = repository.observeSensorState(thresholdsFlow).first()
        assertTrue(firstState is AirSenseState.Stale)
        assertEquals(SensorConnectionSource.NONE, repository.connectionSource.value)
        assertTrue(!repository.isConnected.value)
    }

    @Test
    fun firebaseSensorResponse_mapsEpochCorrectly() {
        val responseWithEpoch = FirebaseSensorResponse(
            temperature = 21.5f,
            humidity = 50.0f,
            gasRaw = 120,
            co = 1.0f,
            ch2o = 0.02f,
            co2 = 420,
            aqi = 25,
            timestamp = 8900L,
            epoch = 1731420500L
        )

        val metrics = responseWithEpoch.toAirQualityMetrics("Excelente")
        assertEquals(1731420500000L, metrics.timestamp)
        assertTrue(metrics.lastUpdated.startsWith("En vivo ("))
    }

    @Test
    fun testFirebaseDirectly_returnsSuccessOnData() = runBlocking {
        val fakeFirebaseApi = object : FirebaseSensorApiService {
            override suspend fun getLiveMetrics(auth: String?): FirebaseSensorResponse {
                return FirebaseSensorResponse(
                    temperature = 23.0f,
                    humidity = 50.0f,
                    co2 = 410,
                    aqi = 20
                )
            }

            override suspend fun getHistory(
                auth: String?,
                orderBy: String?,
                limitToLast: Int?
            ): Map<String, FirebaseSensorResponse>? = null
        }

        val repository = SensorRepository(firebaseSensorApiService = fakeFirebaseApi)
        val result = repository.testFirebaseDirectly("https://test.firebaseio.com")

        assertTrue(result.isSuccess)
        assertEquals(20, result.getOrThrow().aqi)
    }

    @Test
    fun fetchHistoryFromFirebase_returnsSortedMetrics() = runBlocking {
        val fakeFirebaseApi = object : FirebaseSensorApiService {
            override suspend fun getLiveMetrics(auth: String?): FirebaseSensorResponse? = null

            override suspend fun getHistory(
                auth: String?,
                orderBy: String?,
                limitToLast: Int?
            ): Map<String, FirebaseSensorResponse> {
                return mapOf(
                    "-item2" to FirebaseSensorResponse(
                        temperature = 22.0f,
                        humidity = 52.0f,
                        co2 = 460,
                        epoch = 1731420020L
                    ),
                    "-item1" to FirebaseSensorResponse(
                        temperature = 21.0f,
                        humidity = 50.0f,
                        co2 = 450,
                        epoch = 1731420010L
                    )
                )
            }
        }

        val repository = SensorRepository(firebaseSensorApiService = fakeFirebaseApi)
        val result = repository.fetchHistoryFromFirebase(10)

        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(2, list.size)
        assertEquals(1731420010000L, list[0].timestamp)
        assertEquals(1731420020000L, list[1].timestamp)
    }
}
