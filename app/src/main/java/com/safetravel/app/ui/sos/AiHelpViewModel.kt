package com.safetravel.app.ui.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val message: String, val isFromUser: Boolean)

data class AiHelpUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentQuery: String = "",
    val isAwaitingResponse: Boolean = false,
    val emergencyStopped: Boolean = false,
    val passcodeError: String? = null
)

@HiltViewModel
class AiHelpViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AiHelpUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Initial message from the AI
        _uiState.value = _uiState.value.copy(
            messages = listOf(ChatMessage("Help is on the way. How can I assist you while you wait?", false))
        )
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
        if (passcode == "1234") { // Dummy passcode
            // TODO: Send "stop" message to the server
            _uiState.update { it.copy(emergencyStopped = true, passcodeError = null) }
        } else {
            _uiState.update { it.copy(passcodeError = "Invalid passcode.") }
        }
    }
}
