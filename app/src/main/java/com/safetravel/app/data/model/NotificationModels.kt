package com.safetravel.app.data.model

import com.google.gson.annotations.SerializedName

// --- Request Models ---

data class CreateNotificationRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("message") val message: String,
    @SerializedName("is_read") val isRead: Boolean = false
)

data class UpdateNotificationRequest(
    @SerializedName("is_read") val isRead: Boolean
)

// --- Response Models ---

data class NotificationResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("message") val message: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String?
)
