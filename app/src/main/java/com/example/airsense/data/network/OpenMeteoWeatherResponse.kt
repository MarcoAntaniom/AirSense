package com.example.airsense.data.network

import com.google.gson.annotations.SerializedName

data class OpenMeteoWeatherResponse(
    @SerializedName("current")
    val current: WeatherCurrentData? = null
)

data class WeatherCurrentData(
    @SerializedName("temperature_2m")
    val temperature2m: Double? = null,
    @SerializedName("relative_humidity_2m")
    val relativeHumidity2m: Int? = null,
    @SerializedName("weather_code")
    val weatherCode: Int? = null,
    @SerializedName("wind_speed_10m")
    val windSpeed10m: Double? = null
)
