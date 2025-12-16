package com.safetravel.app.data.repository

import com.safetravel.app.data.api.ApiService
import com.safetravel.app.data.model.GetIncidentsResponseDTO
import com.safetravel.app.data.model.IncidentCreateDTO
import com.safetravel.app.data.model.IncidentDTO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {
    private fun getToken(): String {
        return "Bearer ${authRepository.currentToken}"
    }

    suspend fun getIncidents(latitude: Double, longitude: Double, radius: Double): Result<GetIncidentsResponseDTO> {
        return try {
            val response = apiService.getIncidents(getToken(), latitude, longitude, radius)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get incidents: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createIncident(
        title: String,
        description: String,
        category: String,
        latitude: Double,
        longitude: Double,
        severity: Int
    ): Result<IncidentDTO> {
        val request = IncidentCreateDTO(title, description, category, latitude, longitude, severity)
        return try {
            val response = apiService.createIncident(getToken(), request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create incident: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteIncident(incidentId: Long): Result<Unit> {
        return try {
            val response = apiService.deleteIncident(getToken(), incidentId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete incident: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
