package com.safetravel.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.repository.AuthRepository
import com.safetravel.app.data.repository.SettingsRepository
import com.safetravel.app.data.repository.SosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SosState {
    object Idle : SosState()
    object Holding : SosState()
    data class Countdown(val secondsRemaining: Int) : SosState()
    data class PasscodeEntry(val secondsRemaining: Int) : SosState()
    object NavigateToAiHelp : SosState()
}

data class SosUiState(
    val sosState: SosState = SosState.Idle,
    val passcodeError: String? = null,
    val isSending: Boolean = false,
    val sendError: String? = null
)

@HiltViewModel
class SosButtonViewModel @Inject constructor(
    private val sosRepository: SosRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState = _uiState.asStateFlow()

    private var holdJob: Job? = null
    private var countdownJob: Job? = null

    // Retrieve settings lazily when needed
    private suspend fun getSettings() = settingsRepository.settingsFlow.first()

    fun onButtonPress() {
        holdJob = viewModelScope.launch {
            _uiState.update { it.copy(sosState = SosState.Holding) }
            delay(5000)
            startCountdown()
        }
    }

    fun onButtonRelease() {
        holdJob?.cancel()
        if (_uiState.value.sosState is SosState.Holding) {
            _uiState.update { it.copy(sosState = SosState.Idle) }
        }
    }

    private fun startCountdown() {
        if (countdownJob?.isActive == true) return
        countdownJob = viewModelScope.launch {
            val settings = getSettings()
            val startSeconds = settings.countdownTime
            
            for (i in startSeconds downTo 1) {
                val currentState = _uiState.value.sosState
                val newState = when (currentState) {
                    is SosState.Countdown -> SosState.Countdown(i)
                    is SosState.PasscodeEntry -> SosState.PasscodeEntry(i)
                    else -> SosState.Countdown(i)
                }
                _uiState.update { it.copy(sosState = newState) }
                delay(1000)
            }
            sendHelp()
        }
    }

    fun onImOkayClick() {
        val currentState = _uiState.value.sosState
        if (currentState is SosState.Countdown) {
            _uiState.update { it.copy(sosState = SosState.PasscodeEntry(currentState.secondsRemaining), passcodeError = null) }
        }
    }

    fun onPasscodeDialogDismiss() {
        val currentState = _uiState.value.sosState
        if (currentState is SosState.PasscodeEntry) {
            _uiState.update { it.copy(sosState = SosState.Countdown(currentState.secondsRemaining)) }
        }
    }

    fun onVerifyPasscode(passcode: String) {
        viewModelScope.launch {
            val settings = getSettings()
            if (passcode == settings.passcode) {
                countdownJob?.cancel()
                _uiState.update { it.copy(sosState = SosState.Idle) }
            } else {
                _uiState.update { it.copy(passcodeError = "Invalid passcode. Please try again.") }
            }
        }
    }

    fun onSendHelpClick() {
        countdownJob?.cancel()
        sendHelp()
    }

    private fun sendHelp() {
        viewModelScope.launch {
             _uiState.update { it.copy(isSending = true, sendError = null) }
             
             // Hardcoded location for now, should integrate LocationService
             val lat = 34.052235
             val lng = -118.243683
             
             val currentUser = authRepository.currentUser
             val userId = currentUser?.id
             
             if (userId != null) {
                 val result = sosRepository.sendSos(
                     userId = userId,
                     circleId = null, // Backend infers active circle
                     message = "I need help!",
                     lat = lat,
                     lng = lng
                 )
                 
                 if (result.isSuccess) {
                     if (_uiState.value.sosState !is SosState.NavigateToAiHelp) {
                         _uiState.update { it.copy(sosState = SosState.NavigateToAiHelp) }
                     }
                 } else {
                     _uiState.update { it.copy(sendError = result.exceptionOrNull()?.message) }
                      // Still navigate to help even if API fails, better safe than sorry
                      if (_uiState.value.sosState !is SosState.NavigateToAiHelp) {
                         _uiState.update { it.copy(sosState = SosState.NavigateToAiHelp) }
                     }
                 }
             } else {
                  _uiState.update { it.copy(sendError = "User not authenticated") }
                  // If not authenticated, we still probably want to navigate to AI help locally
                  if (_uiState.value.sosState !is SosState.NavigateToAiHelp) {
                       _uiState.update { it.copy(sosState = SosState.NavigateToAiHelp) }
                  }
             }
             
             _uiState.update { it.copy(isSending = false) }
        }
    }

    fun onNavigatedToAiHelp() {
        _uiState.update { it.copy(sosState = SosState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        holdJob?.cancel()
        countdownJob?.cancel()
    }
}
