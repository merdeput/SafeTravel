package com.safetravel.app.data.model

import com.google.gson.annotations.SerializedName

// --- Request Models ---

data class SendSosRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("circle_id") val circleId: Int?, // Optional if backend infers active circle
    @SerializedName("message") val message: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

data class UpdateSosStatusRequest(
    @SerializedName("status") val status: String
)

// --- Response Models ---

data class SosAlertResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("circle_id") val circleId: Int?,
    @SerializedName("message") val message: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String?
)
