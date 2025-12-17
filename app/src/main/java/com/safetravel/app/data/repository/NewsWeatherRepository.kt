package com.safetravel.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.safetravel.app.data.api.ApiService
import com.safetravel.app.data.model.NewsWeatherResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsWeatherRepository @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val cacheDir = File(context.cacheDir, "weather_reports")

    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    suspend fun getWeatherPlace(provinceName: String): Result<NewsWeatherResponse> {
        // 1. Check Cache
        val cached = getFromCache(provinceName)
        if (cached != null) {
            return Result.success(cached)
        }

        // 2. Fetch Network
        return try {
            val response = apiService.getWeatherPlace(provinceName)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                saveToCache(provinceName, body)
                Result.success(body)
            } else {
                Result.failure(Exception("Failed to fetch weather and place info: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFromCache(provinceName: String): NewsWeatherResponse? {
        // Sanitize filename to avoid issues with special characters
        val safeName = provinceName.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        val file = File(cacheDir, "$safeName.json")
        
        if (file.exists()) {
            return try {
                val json = file.readText()
                gson.fromJson(json, NewsWeatherResponse::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        return null
    }

    private fun saveToCache(provinceName: String, data: NewsWeatherResponse) {
        val safeName = provinceName.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        val file = File(cacheDir, "$safeName.json")
        try {
            val json = gson.toJson(data)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
