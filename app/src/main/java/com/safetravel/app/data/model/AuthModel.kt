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
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    // Add other fields if the server returns them
)