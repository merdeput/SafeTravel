package com.safetravel.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.safetravel.app.data.api.ApiService
import com.safetravel.app.data.model.LoginResponse
import com.safetravel.app.data.model.RegisterRequest
import com.safetravel.app.data.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

// Define DataStore for auth preferences
private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {
    
    private object AuthKeys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
    }

    // Simple in-memory storage for the token.
    var currentToken: String? = null
        private set // Only allow setting via login/init
        
    // In-memory storage for current user details
    var currentUser: User? = null
        private set

    init {
        // Load token from DataStore on startup (blocking to ensure it's ready)
        runBlocking {
            currentToken = context.authDataStore.data.map { preferences ->
                preferences[AuthKeys.ACCESS_TOKEN]
            }.first()
        }
        
        if (currentToken != null) {
            Log.d("AuthRepo", "Restored token from storage")
            // Optimistically fetch user data in background
            CoroutineScope(Dispatchers.IO).launch {
                fetchCurrentUser()
            }
        }
    }
    
    suspend fun login(username: String, pass: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(username = username, password = pass)

            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                
                // Save to memory
                currentToken = loginResponse.accessToken 
                
                // Save to DataStore
                context.authDataStore.edit { preferences ->
                    preferences[AuthKeys.ACCESS_TOKEN] = loginResponse.accessToken
                }
                
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

    suspend fun fetchCurrentUser() {
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
            // We ignore errors here because we want to clear local state anyway
            try {
                apiService.logout("Bearer $token")
            } catch (e: Exception) {
                Log.w("AuthRepo", "Server logout failed", e)
            }

            // 2. Clear local token 
            currentToken = null
            currentUser = null
            
            // 3. Clear DataStore
            context.authDataStore.edit { preferences ->
                preferences.remove(AuthKeys.ACCESS_TOKEN)
            }

            Log.d("AuthRepo", "Logout success")
            Result.success(Unit)
            
        } catch (e: Exception) {
            // Should not happen due to inner try-catch, but safe fallback
            currentToken = null
            currentUser = null
            context.authDataStore.edit { preferences ->
                preferences.remove(AuthKeys.ACCESS_TOKEN)
            }
            Log.e("AuthRepo", "Logout exception", e)
            Result.success(Unit)
        }
    }
}
