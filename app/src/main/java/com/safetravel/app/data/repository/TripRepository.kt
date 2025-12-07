package com.safetravel.app.data.repository

import com.safetravel.app.data.api.ApiService
import com.safetravel.app.data.model.TripBase
import com.safetravel.app.data.model.TripDTO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {

    private fun getToken(): String {
        return "Bearer ${authRepository.currentToken}"
    }

    suspend fun getTripById(tripId: Int): Result<TripDTO> {
        return try {
            val response = apiService.getTripById(getToken(), tripId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get trip: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTripsByUser(userId: Int): Result<List<TripDTO>> {
        return try {
            val response = apiService.getTripsByUser(getToken(), userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get user trips: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTrip(tripData: TripBase): Result<TripDTO> {
        return try {
            val response = apiService.createTrip(getToken(), tripData)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create trip: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTrip(tripId: Int, tripData: TripBase): Result<TripDTO> {
        return try {
            val response = apiService.updateTrip(getToken(), tripId, tripData)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update trip: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTrip(tripId: Int): Result<Unit> {
        return try {
            val response = apiService.deleteTrip(getToken(), tripId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete trip: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
