package com.safetravel.app.data.repository

import com.safetravel.app.data.api.ApiService
import com.safetravel.app.data.model.SendSosRequest
import com.safetravel.app.data.model.SosAlertResponse
import com.safetravel.app.data.model.UpdateSosStatusRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SosRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {

    private fun getToken(): String {
        return "Bearer ${authRepository.currentToken}"
    }

    suspend fun sendSos(userId: Int, circleId: Int?, message: String, lat: Double, lng: Double): Result<SosAlertResponse> {
        return try {
            val request = SendSosRequest(userId, circleId, message, lat, lng)
            val response = apiService.sendSos(getToken(), request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to send SOS: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSosStatus(alertId: Int, status: String): Result<SosAlertResponse> {
        return try {
            val response = apiService.updateSosStatus(getToken(), alertId, UpdateSosStatusRequest(status))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update SOS status: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMySosAlerts(): Result<List<SosAlertResponse>> {
        return try {
            val response = apiService.getMySosAlerts(getToken())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get SOS alerts: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
