package com.safetravel.app.data.repository

import android.util.Log
import com.safetravel.app.data.api.ApiService
import com.safetravel.app.data.model.LoginResponse
import com.safetravel.app.data.model.RegisterRequest
import com.safetravel.app.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService
) {
    // Simple in-memory storage for the token.
    // In a real production app, you would save this to EncryptedSharedPreferences or DataStore.
    var currentToken: String? = null
        private set // Only allow setting via login
        
    // In-memory storage for current user details
    var currentUser: User? = null
        private set

    suspend fun login(username: String, pass: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(username = username, password = pass)

            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                currentToken = loginResponse.accessToken // Save the token
                
                // Fetch user details immediately after login
                fetchCurrentUser()
                
                Log.d("AuthRepo", "Login success, token saved: ${currentToken?.take(10)}...")
                Result.success(loginResponse)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown login error"
                Log.e("AuthRepo", "Login failed: $errorMsg")
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Login exception", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchCurrentUser() {
        val token = currentToken ?: return
        try {
            val response = apiService.getCurrentUser("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                currentUser = response.body()
                Log.d("AuthRepo", "Fetched user ID: ${currentUser?.id}")
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Failed to fetch current user", e)
        }
    }

    suspend fun register(request: RegisterRequest): Result<User> {
        return try {
            val response = apiService.register(request)

            if (response.isSuccessful && response.body() != null) {
                Log.d("AuthRepo", "Registration success")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AuthRepo", "Register error: $errorBody")
                Result.failure(Exception("Registration failed: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Register exception", e)
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        val token = currentToken
        if (token == null) {
            // Already logged out locally
            return Result.success(Unit)
        }

        return try {
            // 1. Call the server with "Bearer TOKEN" to invalidate session
            val response = apiService.logout("Bearer $token")

            // 2. Clear local token regardless of server success
            // (if server is down, we still want the user to be able to logout locally)
            currentToken = null
            currentUser = null

            if (response.isSuccessful) {
                Log.d("AuthRepo", "Server logout success")
                Result.success(Unit)
            } else {
                Log.w("AuthRepo", "Server logout failed: ${response.code()}")
                // We still consider it a "success" for the app flow because the local token is gone
                Result.success(Unit)
            }
        } catch (e: Exception) {
            // Even if network fails, we clear the token locally so the user isn't stuck
            currentToken = null
            currentUser = null
            Log.e("AuthRepo", "Logout network exception", e)
            Result.success(Unit)
        }
    }
}
