package com.safetravel.app.data.model

import com.google.gson.annotations.SerializedName

data class LocationData(
    @SerializedName("type") val type: String,
    @SerializedName("coordinates") val coordinates: Coordinates,
    @SerializedName("accuracy") val accuracy: Float? = null,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("place_name") val placeName: String? = null
)

data class Coordinates(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lng: Double
)