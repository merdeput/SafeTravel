package com.safetravel.app.data.repository

import com.safetravel.app.data.api.ApiService
import com.safetravel.app.data.model.AddCircleMemberRequest
import com.safetravel.app.data.model.CircleMemberResponse
import com.safetravel.app.data.model.CircleResponse
import com.safetravel.app.data.model.CreateCircleRequest
import com.safetravel.app.data.model.UpdateCircleRequest
import com.safetravel.app.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CircleRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {

    private fun getToken(): String {
        return "Bearer ${authRepository.currentToken}"
    }

    suspend fun createCircle(name: String, description: String): Result<CircleResponse> {
        return try {
            val response = apiService.createCircle(getToken(), CreateCircleRequest(name, description))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create circle: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCircles(): Result<List<CircleResponse>> {
        return try {
            val response = apiService.getCircles(getToken())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get circles: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCircle(circleId: Int): Result<CircleResponse> {
        return try {
            val response = apiService.getCircle(getToken(), circleId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get circle: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCircle(circleId: Int, name: String, status: String? = null): Result<CircleResponse> {
        return try {
            val response = apiService.updateCircle(getToken(), circleId, UpdateCircleRequest(name, status))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update circle: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCircle(circleId: Int): Result<Unit> {
        return try {
            val response = apiService.deleteCircle(getToken(), circleId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete circle: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCircleMember(circleId: Int, userId: Int, role: String = "member"): Result<CircleMemberResponse> {
        return try {
            // Fix: Construct the request with all 3 required parameters to avoid compile error and 422
            val request = AddCircleMemberRequest(circleId = circleId, memberId = userId, role = role)
            val response = apiService.addCircleMember(getToken(), circleId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to add member: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCircleMembers(circleId: Int): Result<List<User>> {
        return try {
            val response = apiService.getCircleMembers(getToken(), circleId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get circle members: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeCircleMember(circleId: Int, userId: Int): Result<Unit> {
        return try {
            val response = apiService.removeCircleMember(getToken(), circleId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove member: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
