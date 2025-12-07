package com.safetravel.app.data.model

import com.google.gson.annotations.SerializedName

// Constant for "null" date when backend requires a value
const val SPECIAL_END_DATE = "0001-01-01T00:00:00"

// --- Request Models ---

data class TripBase(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("tripname") val tripName: String,
    @SerializedName("destination") val destination: String,
    @SerializedName("start_date") val startDate: String, // ISO 8601 string
    @SerializedName("end_date") val endDate: String? = null,   // ISO 8601 string, nullable now
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("trip_type") val tripType: String,
    @SerializedName("have_elderly") val haveElderly: Boolean = false,
    @SerializedName("have_children") val haveChildren: Boolean = false,
    @SerializedName("circle_id") val circleId: Int? = null
)

// --- Response Models ---

data class TripDTO(
    @SerializedName("id") val id: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("tripname") val tripName: String,
    @SerializedName("destination") val destination: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String?, // Nullable
    @SerializedName("notes") val notes: String?,
    @SerializedName("trip_type") val tripType: String,
    @SerializedName("have_elderly") val haveElderly: Boolean,
    @SerializedName("have_children") val haveChildren: Boolean,
    @SerializedName("circle_id") val circleId: Int?
)
