package com.safetravel.app.data.api

import com.safetravel.app.data.model.LocationData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("location")
    suspend fun sendLocation(@Body locationData: LocationData): Response<Map<String, Any>>
}
