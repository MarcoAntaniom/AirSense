package com.example.airsense.data.network

import com.google.gson.annotations.SerializedName

data class GoogleAirQualityRequest(
    @SerializedName("location")
    val location: LatLngLocation,
    @SerializedName("extraComputations")
    val extraComputations: List<String> = listOf("LOCAL_AQI", "POLLUTANT_CONCENTRATION")
)

data class LatLngLocation(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double
)
