package com.safetravel.app.data.model

import com.google.gson.annotations.SerializedName

// What we send to /api/register
data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("avatar_url") val avatarUrl: String = "https://example.com/default_avatar.jpg" // Default
)

// What we get back from /api/login
data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class User(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("username") val username: String? = null, // Made nullable to prevent parsing errors
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)
