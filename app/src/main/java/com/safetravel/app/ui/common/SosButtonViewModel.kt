package com.safetravel.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val passcodeError: String? = null
)

@HiltViewModel
class SosButtonViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState = _uiState.asStateFlow()

    private var holdJob: Job? = null
    private var countdownJob: Job? = null

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
            for (i in 30 downTo 1) {
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
        if (passcode == "1234") { // Dummy passcode
            countdownJob?.cancel()
            _uiState.update { it.copy(sosState = SosState.Idle) }
        } else {
            _uiState.update { it.copy(passcodeError = "Invalid passcode. Please try again.") }
        }
    }

    fun onSendHelpClick() {
        countdownJob?.cancel()
        sendHelp()
    }

    private fun sendHelp() {
        if (_uiState.value.sosState is SosState.NavigateToAiHelp) return
        _uiState.update { it.copy(sosState = SosState.NavigateToAiHelp) }
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
