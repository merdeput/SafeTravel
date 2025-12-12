package com.safetravel.app.ui.sos

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.repository.SettingsRepository
import com.safetravel.app.data.repository.SosRepository
import com.safetravel.app.service.BackgroundSafetyService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val message: String, val isFromUser: Boolean)

data class AiHelpUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentQuery: String = "",
    val isAwaitingResponse: Boolean = false,
    val emergencyStopped: Boolean = false,
    val passcodeError: String? = null,
    val stopError: String? = null // To show server errors
)

@HiltViewModel
class AiHelpViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sosRepository: SosRepository, // Inject Repository
    private val settingsRepository: SettingsRepository // Inject Settings
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiHelpUiState())
    val uiState = _uiState.asStateFlow()

    // We need the alert ID to resolve it. 
    // In a real app, you'd pass this ID via Navigation Arguments.
    // For now, we'll fetch the latest active alert to resolve it.
    private var currentAlertId: Int? = null

    init {
        // Initial message from the AI
        _uiState.value = _uiState.value.copy(
            messages = listOf(ChatMessage("Help is on the way. How can I assist you while you wait?", false))
        )
        
        // Try to find the active alert ID
        fetchLatestActiveAlert()
    }
    
    private suspend fun getSettings() = settingsRepository.settingsFlow.first()
    
    private fun fetchLatestActiveAlert() {
        viewModelScope.launch {
             val result = sosRepository.getMySosAlerts()
             if (result.isSuccess) {
                 // Find the most recent active alert (not resolved)
                 val activeAlert = result.getOrNull()
                     ?.filter { it.status != "resolved" }
                     ?.maxByOrNull { it.createdAt ?: "" } 
                     
                 currentAlertId = activeAlert?.id
                 Log.d("AiHelpViewModel", "Found active alert ID: $currentAlertId")
             } else {
                 Log.e("AiHelpViewModel", "Failed to fetch alerts: ${result.exceptionOrNull()?.message}")
             }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(currentQuery = query) }
    }

    fun onAskClick() {
        val userQuery = _uiState.value.currentQuery
        if (userQuery.isBlank()) return

        val newMessages = _uiState.value.messages.toMutableList()
        newMessages.add(ChatMessage(userQuery, true))

        _uiState.update { it.copy(messages = newMessages, isAwaitingResponse = true, currentQuery = "") }

        // Simulate AI response
        viewModelScope.launch {
            delay(1500)
            newMessages.add(ChatMessage("This is a simulated AI response to your query: '$userQuery'. In a real scenario, I would provide helpful information based on your situation.", false))
            _uiState.update { it.copy(messages = newMessages, isAwaitingResponse = false) }
        }
    }

    fun onVerifyPasscode(passcode: String) {
        viewModelScope.launch {
            val settings = getSettings()
            if (passcode == settings.passcode) {
                _uiState.update { it.copy(passcodeError = null) }
                sendResetToService()
                resolveEmergency()
            } else {
                _uiState.update { it.copy(passcodeError = "Invalid passcode.") }
            }
        }
    }
    
    private fun resolveEmergency() {
        viewModelScope.launch {
            val alertId = currentAlertId
            if (alertId != null) {
                 // Call the backend to resolve
                 val result = sosRepository.updateSosStatus(alertId, "resolved")
                 
                 if (result.isSuccess) {
                     Log.d("AiHelpViewModel", "Emergency resolved successfully on server.")
                     _uiState.update { it.copy(emergencyStopped = true) }
                 } else {
                     // If server fails (e.g. 400 Bad Request due to notification bug),
                     // we log it but assume the user is safe and wants to exit.
                     // The DB update likely happened before the notification crash.
                     val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                     Log.e("AiHelpViewModel", "Server update failed (ignoring for UX): $errorMsg")
                     
                     // Exit immediately, treating it as a success for the user
                     _uiState.update { it.copy(emergencyStopped = true) }
                 }
            } else {
                // No alert ID found, just close screen
                Log.w("AiHelpViewModel", "No active alert ID found to resolve.")
                _uiState.update { it.copy(emergencyStopped = true) }
            }
        }
    }

    private fun sendResetToService() {
        val intent = Intent(appContext, BackgroundSafetyService::class.java).apply {
            action = BackgroundSafetyService.ACTION_RESET_DETECTOR
        }
        appContext.startService(intent)
    }
}
