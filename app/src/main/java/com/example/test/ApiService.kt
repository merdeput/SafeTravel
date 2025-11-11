package com.example.test

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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

interface ApiService {
    @POST("location")
    suspend fun sendLocation(@Body locationData: LocationData): retrofit2.Response<Map<String, Any>>
}

object ApiClient {
    private const val BASE_URL = "http://192.168.1.127:5000/" // Change this!

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}