package com.safetravel.app.data.repository

import com.safetravel.app.data.api.ApiService
import com.safetravel.app.data.model.NewsWeatherResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsWeatherRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {
    suspend fun getNewsAndWeather(location: String): Result<NewsWeatherResponse> {
        val token = authRepository.currentToken ?: return Result.failure(Exception("Not logged in"))
        return try {
            val response = apiService.getNewsAndWeather("Bearer $token", location)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch news and weather: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
