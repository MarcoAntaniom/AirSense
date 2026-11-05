package com.example.airsense.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.airsense.data.models.AirQualityMetrics
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Payload JSON emitido por la placa WeMos D1 (ESP8266) en modo Access Point.
 */
data class LocalSensorResponse(
    @SerializedName("temperature") val temperature: Float = 0f,
    @SerializedName("humidity") val humidity: Float = 0f,
    @SerializedName("gasRaw") val gasRaw: Int = 0,
    @SerializedName("co") val co: Float = 0f,
    @SerializedName("ch2o") val ch2o: Float = 0f,
    @SerializedName("co2") val co2: Int = 0,
    @SerializedName("aqi") val aqi: Int = 0,
    @SerializedName("timestamp") val timestamp: Long = 0L
) {
    /**
     * Mapea la respuesta de la placa WeMos D1 al modelo de dominio AirQualityMetrics.
     * Reemplaza el millis() de la placa ("timestamp" en JSON) asignando System.currentTimeMillis()
     * para conservar la hora real del sistema Android.
     */
    fun toAirQualityMetrics(aqiStatus: String = "Excelente"): AirQualityMetrics {
        val now = System.currentTimeMillis()
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
 * Cliente HTTP para comunicación local con el Access Point WiFi "AirSense_AP" en 192.168.4.1.
 */
interface LocalSensorApiService {

    @GET("data")
    suspend fun getSensorData(): LocalSensorResponse

    companion object {
        const val BASE_URL = "http://192.168.4.1/"

        fun create(context: Context? = null): LocalSensorApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val clientBuilder = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(2500, TimeUnit.MILLISECONDS)
                .readTimeout(2500, TimeUnit.MILLISECONDS)

            if (context != null) {
                bindWifiNetwork(context, clientBuilder)
            }

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LocalSensorApiService::class.java)
        }

        /**
         * Enrutamiento forzado a la interfaz WiFi (AirSense_AP).
         * Dado que la red WiFi local del WeMos D1 no posee salida a internet, Android desvía
         * por defecto el tráfico HTTP hacia los datos móviles (4G/5G).
         * Este método busca la red activa con TRANSPORT_WIFI y asigna su socketFactory al OkHttpClient.
         */
        fun bindWifiNetwork(context: Context, clientBuilder: OkHttpClient.Builder) {
            try {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                connectivityManager?.let { cm ->
                    val wifiNetwork = cm.allNetworks.firstOrNull { network ->
                        val caps = cm.getNetworkCapabilities(network)
                        caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    } ?: cm.activeNetwork?.takeIf { network ->
                        val caps = cm.getNetworkCapabilities(network)
                        caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    }
                    if (wifiNetwork != null) {
                        clientBuilder.socketFactory(wifiNetwork.socketFactory)
                    }
                }
            } catch (_: Exception) {
                // Silencioso en entornos de test o sin permisos de conectividad
            }
        }
    }
}
