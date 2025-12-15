package com.safetravel.app.data.repository

import android.util.Log
import com.safetravel.app.data.api.ApiService
import com.safetravel.app.data.model.FriendRequest
import com.safetravel.app.data.model.FriendRequestRequest
import com.safetravel.app.data.model.Friendship
import com.safetravel.app.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {

    private fun getToken(): String {
        return "Bearer ${authRepository.currentToken}"
    }

    suspend fun sendFriendRequest(receiverUsername: String): Result<FriendRequest> {
        return try {
            val response = apiService.sendFriendRequest(getToken(), FriendRequestRequest(receiverUsername))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to send friend request: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingFriendRequests(): Result<List<FriendRequest>> {
        return try {
            val response = apiService.getPendingFriendRequests(getToken())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get pending requests: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requestId: Int): Result<Friendship> {
        return try {
            val response = apiService.acceptFriendRequest(getToken(), requestId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to accept request: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(requestId: Int): Result<FriendRequest> {
        return try {
            val response = apiService.rejectFriendRequest(getToken(), requestId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to reject request: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFriends(): Result<List<User>> {
        return try {
            val response = apiService.getFriends(getToken())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get friends: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteFriend(friendId: Int): Result<Unit> {
        return try {
            val response = apiService.deleteFriend(getToken(), friendId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete friend: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserById(userId: Int): Result<User> {
        return try {
            val response = apiService.getUserById(getToken(), userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get user details: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
