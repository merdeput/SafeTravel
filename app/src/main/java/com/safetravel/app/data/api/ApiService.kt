package com.safetravel.app.data.api

import com.safetravel.app.data.model.LocationData
import com.safetravel.app.data.model.LoginResponse
import com.safetravel.app.data.model.RegisterRequest
import com.safetravel.app.data.model.User
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("location")
    suspend fun sendLocation(@Body locationData: LocationData): Response<Map<String, Any>>

    // --- AUTH ENDPOINTS ---

    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<User>

    @FormUrlEncoded
    @POST("api/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    // Add this for Logout!
    @POST("api/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>> // Server returns a JSON message
}