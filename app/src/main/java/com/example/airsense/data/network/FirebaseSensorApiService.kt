package com.example.airsense.data.network

import com.example.airsense.data.models.AirQualityMetrics
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Payload JSON recibido automáticamente desde Google Firebase Realtime Database (/airsense/actual.json)
 * emitido por el microcontrolador ESP8266 vía Internet.
 */
data class FirebaseSensorResponse(
    @SerializedName("temperature") val temperature: Float = 0f,
    @SerializedName("humidity") val humidity: Float = 0f,
    @SerializedName("gasRaw") val gasRaw: Int = 0,
    @SerializedName("co") val co: Float = 0f,
    @SerializedName("ch2o") val ch2o: Float = 0f,
    @SerializedName("co2") val co2: Int = 0,
    @SerializedName("aqi") val aqi: Int = 0,
    @SerializedName("timestamp") val timestamp: Long = 0L,
    @SerializedName("epoch") val epoch: Long? = null
) {
    fun toAirQualityMetrics(aqiStatus: String = "Excelente"): AirQualityMetrics {
        val now = if (epoch != null && epoch > 1000000000L) {
            epoch * 1000L
        } else {
            System.currentTimeMillis()
        }
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val lastUpdatedStr = "En vivo (${timeFormat.format(Date(now))})"

        return AirQualityMetrics(
            aqi = aqi,
            aqiStatus = aqiStatus,
            temperature = temperature,
            humidity = humidity,
            gasRaw = gasRaw,
            co = co,
            ch2o = ch2o,
            co2 = co2,
            timestamp = now,
            lastUpdated = lastUpdatedStr,
            isMawCalibrated = true
        )
    }
}

/**
 * Cliente HTTP REST para comunicación automática con Google Firebase Realtime Database.
 */
interface FirebaseSensorApiService {

    @GET("airsense/actual.json")
    suspend fun getLiveMetrics(
        @Query("auth") auth: String? = null
    ): FirebaseSensorResponse?

    @GET("airsense/historial.json")
    suspend fun getHistory(
        @Query("auth") auth: String? = null,
        @Query("orderBy") orderBy: String? = null,
        @Query("limitToLast") limitToLast: Int? = null
    ): Map<String, FirebaseSensorResponse>?

    companion object {
        const val FIREBASE_HOST_URL = "https://airsense-c02fa-default-rtdb.firebaseio.com/"

        fun create(rawHostUrl: String = FIREBASE_HOST_URL): FirebaseSensorApiService {
            val formattedUrl = if (rawHostUrl.endsWith("/")) rawHostUrl else "$rawHostUrl/"

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(4000, TimeUnit.MILLISECONDS)
                .readTimeout(4000, TimeUnit.MILLISECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(formattedUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FirebaseSensorApiService::class.java)
        }
    }
}
