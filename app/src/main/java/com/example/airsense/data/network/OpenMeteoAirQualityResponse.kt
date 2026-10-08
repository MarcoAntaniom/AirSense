package com.example.airsense.data.network

import com.google.gson.annotations.SerializedName

data class OpenMeteoAirQualityResponse(
    @SerializedName("current")
    val current: AirQualityCurrentData? = null
)

data class AirQualityCurrentData(
    @SerializedName("european_aqi")
    val europeanAqi: Int? = null,
    @SerializedName("us_aqi")
    val usAqi: Int? = null,
    @SerializedName("pm2_5")
    val pm25: Double? = null,
    @SerializedName("pm10")
    val pm10: Double? = null,
    @SerializedName("carbon_monoxide")
    val carbonMonoxide: Double? = null
)
