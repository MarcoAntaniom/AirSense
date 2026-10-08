package com.example.airsense.data.network

import com.google.gson.annotations.SerializedName

data class GoogleAirQualityResponse(
    @SerializedName("dateTime")
    val dateTime: String? = null,
    @SerializedName("indexes")
    val indexes: List<GoogleAqiIndex>? = null,
    @SerializedName("pollutants")
    val pollutants: List<GooglePollutant>? = null
)

data class GoogleAqiIndex(
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("displayName")
    val displayName: String? = null,
    @SerializedName("aqi")
    val aqi: Int? = null,
    @SerializedName("category")
    val category: String? = null
)

data class GooglePollutant(
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("displayName")
    val displayName: String? = null,
    @SerializedName("concentration")
    val concentration: GoogleConcentration? = null
)

data class GoogleConcentration(
    @SerializedName("value")
    val value: Double? = null,
    @SerializedName("units")
    val units: String? = null
)
