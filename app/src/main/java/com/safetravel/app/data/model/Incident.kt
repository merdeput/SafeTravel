package com.safetravel.app.data.model

import com.google.gson.annotations.SerializedName

data class IncidentDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("category") val category: String?, // e.g., "accident"
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("severity") val severity: Int?,
    @SerializedName("created_at") val createdAt: String,
    // Fields for SOS type incidents
    @SerializedName("user_id") val userId: Long? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("user") val user: User? = null,
    @SerializedName("status") val status: String? = null // e.g., "active", "resolved"
)

data class IncidentCreateDTO(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("severity") val severity: Int
)

data class IncidentItemWrapper(
    @SerializedName("priority") val priority: Int, // 0=Red(SOS Friend), 1=Yellow(SOS Other), 2=Blue(Report)
    @SerializedName("item") val item: IncidentDTO
)

data class GetIncidentsResponseDTO(
    @SerializedName("items") val items: List<IncidentItemWrapper>
)
