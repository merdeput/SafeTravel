package com.safetravel.app.data.model

import com.google.gson.annotations.SerializedName

// --- Request Models ---

data class CreateCircleRequest(
    @SerializedName("circle_name") val circleName: String,
    @SerializedName("description") val description: String? = null
)

data class UpdateCircleRequest(
    @SerializedName("circle_name") val circleName: String,
    @SerializedName("status") val status: String? = null
)

data class AddCircleMemberRequest(
    @SerializedName("circle_id") val circleId: Int,
    @SerializedName("member_id") val memberId: Int,
    @SerializedName("role") val role: String = "member"
)

// --- Response Models ---

data class CircleResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("circle_name") val circleName: String,
    @SerializedName("description") val description: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class CircleMemberResponse(
    @SerializedName("id") val id: Int, 
    @SerializedName("circle_id") val circleId: Int,
    @SerializedName("user_id") val userId: Int, // backend likely returns user_id
    @SerializedName("role") val role: String,
    @SerializedName("joined_at") val joinedAt: String?
)
