package com.safetravel.app.data.repository

import com.safetravel.app.data.api.ApiService
import com.safetravel.app.data.model.CreateNotificationRequest
import com.safetravel.app.data.model.NotificationResponse
import com.safetravel.app.data.model.UpdateNotificationRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: ApiService,
    private val authRepository: AuthRepository
) {

    private fun getToken(): String {
        return "Bearer ${authRepository.currentToken}"
    }

    suspend fun createNotification(userId: Int, message: String): Result<NotificationResponse> {
        return try {
            val response = apiService.createNotification(getToken(), CreateNotificationRequest(userId, message))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create notification: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotifications(): Result<List<NotificationResponse>> {
        return try {
            val response = apiService.getNotifications(getToken())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get notifications: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: Int): Result<NotificationResponse> {
        return try {
            val response = apiService.updateNotification(getToken(), notificationId, UpdateNotificationRequest(isRead = true))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to mark notification as read: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: Int): Result<Unit> {
        return try {
            val response = apiService.deleteNotification(getToken(), notificationId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete notification: ${response.code()} - ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
