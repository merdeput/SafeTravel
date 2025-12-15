package com.safetravel.app.ui.sos

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.safetravel.app.BuildConfig
import com.safetravel.app.data.repository.BluetoothBleManager
import com.safetravel.app.data.repository.SettingsRepository
import com.safetravel.app.data.repository.SosRepository
import com.safetravel.app.service.BackgroundSafetyService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val stopError: String? = null,
    val isRecording: Boolean = false,
    val isSpeaking: Boolean = false,
    val voiceEnabled: Boolean = true,
    val isAdvertisingBle: Boolean = false
)

@HiltViewModel
class AiHelpViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sosRepository: SosRepository,
    private val settingsRepository: SettingsRepository,
    private val bluetoothBleManager: BluetoothBleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiHelpUiState())
    val uiState = _uiState.asStateFlow()

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content {
                text("""You are an emergency safety assistant for SafeTravel app. Your role is to provide ACTIONABLE guidance in crisis situations.

RESPONSE RULES:
• Use bullet points (•) for all multi-step instructions
• Maximum 3-4 bullet points per response
• Each bullet: 1 short, clear action (5-10 words)
• NO empathy phrases ("I understand", "I'm sorry", "Stay calm")
• NO questions unless absolutely necessary for safety
• Be direct and authoritative

PRIORITY ACTIONS:
• Life-threatening situations → "Call 113 (Police) or 115 (Ambulance) NOW"
• Injuries → Specific first aid steps only
• Unsafe location → Immediate relocation instructions
• Threats → Concrete safety measures

FORMAT EXAMPLES:
Bad: "I understand you're scared. It's important to stay calm. You should try to find a safe place."
Good: 
• Move to nearest public area with people
• Keep phone ready to call 113
• Avoid dark/isolated spaces

Bad: "Can you tell me more about what's happening? I want to help you feel better."
Good:
• Share your live location with emergency contact
• Note nearby landmarks/street signs
• Stay on main roads

MEDICAL GUIDANCE:
• Only basic first aid (bleeding, choking, burns)
• Always end with: "Call 115 for medical help"
• Never diagnose or give treatment advice

Keep responses under 40 words total. Action over comfort.""")
            }
        )
    }

    private var currentAlertId: Int? = null

    init {
        _uiState.value = _uiState.value.copy(
            messages = listOf(ChatMessage("Help is on the way. How can I assist you while you wait?", false))
        )
        fetchLatestActiveAlert()
        
        // Start BLE advertising automatically
        startBleAdvertising()
    }

    private suspend fun getSettings() = settingsRepository.settingsFlow.first()

    private fun fetchLatestActiveAlert() {
        viewModelScope.launch {
            val result = sosRepository.getMySosAlerts()
            if (result.isSuccess) {
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
    
    private fun startBleAdvertising() {
        bluetoothBleManager.startAdvertising()
        _uiState.update { it.copy(isAdvertisingBle = true) }
    }
    
    fun toggleBleAdvertising() {
        if (_uiState.value.isAdvertisingBle) {
            bluetoothBleManager.stopAdvertising()
            _uiState.update { it.copy(isAdvertisingBle = false) }
        } else {
            bluetoothBleManager.startAdvertising()
            _uiState.update { it.copy(isAdvertisingBle = true) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(currentQuery = query) }
    }

    fun setRecordingState(isRecording: Boolean) {
        _uiState.update { it.copy(isRecording = isRecording) }
    }

    fun setSpeakingState(isSpeaking: Boolean) {
        _uiState.update { it.copy(isSpeaking = isSpeaking) }
    }

    fun onTranscriptionReceived(transcription: String) {
        if (transcription.isBlank()) return

        // Add user message from voice
        val newMessages = _uiState.value.messages.toMutableList()
        newMessages.add(ChatMessage(transcription, true))
        _uiState.update { it.copy(messages = newMessages, currentQuery = "") }

        // Process the transcription
        sendMessageToAI(transcription)
    }

    fun onAskClick() {
        val userQuery = _uiState.value.currentQuery
        if (userQuery.isBlank()) return

        val newMessages = _uiState.value.messages.toMutableList()
        newMessages.add(ChatMessage(userQuery, true))
        _uiState.update { it.copy(messages = newMessages, currentQuery = "") }

        sendMessageToAI(userQuery)
    }

    private fun sendMessageToAI(userQuery: String) {
        _uiState.update { it.copy(isAwaitingResponse = true) }

        viewModelScope.launch {
            try {
                if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                    throw Exception("API Key is missing. Please add GEMINI_API_KEY to local.properties.")
                }

                val history = _uiState.value.messages.dropLast(1).map {
                    content(role = if (it.isFromUser) "user" else "model") { text(it.message) }
                }

                val chat = generativeModel.startChat(history = history)
                val response = chat.sendMessage(userQuery)
                val responseText = response.text ?: "I couldn't generate a response."

                val updatedMessages = _uiState.value.messages.toMutableList()
                updatedMessages.add(ChatMessage(responseText, false))
                _uiState.update { it.copy(messages = updatedMessages, isAwaitingResponse = false) }

            } catch (e: Exception) {
                Log.e("AiHelpViewModel", "Gemini API Error", e)
                val errorMessage = e.localizedMessage ?: "Unknown error"
                val updatedMessages = _uiState.value.messages.toMutableList()

                val displayError = when {
                    errorMessage.contains("API Key") -> "Configuration Error: API Key missing."
                    errorMessage.contains("401") -> "Authentication Error: Invalid API Key."
                    errorMessage.contains("Unable to resolve host") -> "No Internet Connection. Try sending SMS to your contacts."
                    errorMessage.contains("not found") -> "Model not found. Please check API settings."
                    else -> "Connection error: $errorMessage"
                }

                updatedMessages.add(ChatMessage(displayError, false))
                _uiState.update { it.copy(messages = updatedMessages, isAwaitingResponse = false) }
            }
        }
    }

    fun onVerifyPasscode(passcode: String) {
        viewModelScope.launch {
            val settings = getSettings()
            if (passcode == settings.passcode) {
                _uiState.update { it.copy(passcodeError = null) }
                sendResetToService()
                resolveEmergency()
                bluetoothBleManager.stopAdvertising() // Stop BLE on resolve
            } else {
                _uiState.update { it.copy(passcodeError = "Invalid passcode.") }
            }
        }
    }

    private fun resolveEmergency() {
        viewModelScope.launch {
            val alertId = currentAlertId
            if (alertId != null) {
                val result = sosRepository.updateSosStatus(alertId, "resolved")

                if (result.isSuccess) {
                    Log.d("AiHelpViewModel", "Emergency resolved successfully on server.")
                    _uiState.update { it.copy(emergencyStopped = true) }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("AiHelpViewModel", "Server update failed (ignoring for UX): $errorMsg")
                    _uiState.update { it.copy(emergencyStopped = true) }
                }
            } else {
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
    
    override fun onCleared() {
        super.onCleared()
        // Ensure advertising stops when ViewModel is cleared/screen closed, 
        // unless you want it persistent even if user exits app (in which case move control to Service)
        // For now, let's keep it running unless explicitly stopped via 'resolveEmergency'
    }
}
