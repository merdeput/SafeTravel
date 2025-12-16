package com.safetravel.app.ui.common

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.repository.AuthRepository
import com.safetravel.app.data.repository.LocationService
import com.safetravel.app.data.repository.SensorDataRepository
import com.safetravel.app.data.repository.SettingsRepository
import com.safetravel.app.data.repository.SosRepository
import com.safetravel.app.service.BackgroundSafetyService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val appContext: Context,
    private val sosRepository: SosRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val sensorDataRepository: SensorDataRepository,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState = _uiState.asStateFlow()

    private var holdJob: Job? = null
    private var countdownJob: Job? = null
    private var pendingMessage: String = "I need help!"
    private var lastDismissedAt: Long = 0L

    private enum class SosTrigger { MANUAL, ACCIDENT, FALL, VOLUME }

    // Retrieve settings lazily when needed
    private suspend fun getSettings() = settingsRepository.settingsFlow.first()

    init {
        // Auto-start the SOS countdown when detectors flag an accident/fall
        viewModelScope.launch {
            sensorDataRepository.detectorEvents.collect { trigger ->
                when (trigger) {
                    SensorDataRepository.DetectorTrigger.Accident -> startCountdownFromTrigger(SosTrigger.ACCIDENT)
                    SensorDataRepository.DetectorTrigger.Fall -> startCountdownFromTrigger(SosTrigger.FALL)
                    SensorDataRepository.DetectorTrigger.VolumeSos -> startCountdownFromTrigger(SosTrigger.VOLUME)
                }
            }
        }
        
        // Listen for Reset events (e.g., stopped from EmergencyActivity or Service)
        viewModelScope.launch {
            sensorDataRepository.resetEvents.collect {
                // If reset event occurs, clear any active SOS UI/Countdown
                if (_uiState.value.sosState !is SosState.Idle) {
                    countdownJob?.cancel()
                    holdJob?.cancel()
                    _uiState.update { it.copy(sosState = SosState.Idle, passcodeError = null) }
                }
            }
        }
    }

    fun onButtonPress() {
        holdJob = viewModelScope.launch {
            _uiState.update { it.copy(sosState = SosState.Holding) }
            delay(5000)
            startCountdown(SosTrigger.MANUAL)
        }
    }

    fun onButtonRelease() {
        holdJob?.cancel()
        if (_uiState.value.sosState is SosState.Holding) {
            _uiState.update { it.copy(sosState = SosState.Idle) }
        }
    }

    private fun startCountdownFromTrigger(trigger: SosTrigger) {
        // Avoid restarting if a countdown is already running
        val currentState = _uiState.value.sosState
        if (countdownJob?.isActive == true ||
            currentState is SosState.Countdown ||
            currentState is SosState.PasscodeEntry ||
            isInDismissCooldown()
        ) return

        holdJob?.cancel()
        viewModelScope.launch { startCountdown(trigger) }
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
                sendResetToService()
                lastDismissedAt = System.currentTimeMillis()
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

    private suspend fun startCountdown(trigger: SosTrigger) {
        if (countdownJob?.isActive == true) return

        val settings = getSettings()
        val startSeconds = settings.countdownTime

        pendingMessage = when (trigger) {
            SosTrigger.MANUAL -> "I need help!"
            SosTrigger.ACCIDENT -> "Accident detected. I need assistance."
            SosTrigger.FALL -> "Fall detected. I need assistance."
            SosTrigger.VOLUME -> "SOS triggered via volume buttons. I need assistance."
        }

        _uiState.update { it.copy(sosState = SosState.Countdown(startSeconds), passcodeError = null) }

        countdownJob = viewModelScope.launch {
            for (i in startSeconds downTo 1) {
                val currentState = _uiState.value.sosState
                val newState = when (currentState) {
                    is SosState.PasscodeEntry -> SosState.PasscodeEntry(i)
                    else -> SosState.Countdown(i)
                }
                _uiState.update { it.copy(sosState = newState) }
                delay(1000)
            }
            sendHelp()
        }
    }

    private fun sendHelp() {
        viewModelScope.launch {
             _uiState.update { it.copy(isSending = true, sendError = null) }
             
             // Get current location from LocationService
             val location = locationService.getCurrentLocation()
             
             // Use actual location or fallback if null (0.0, 0.0 is often used or maybe last known)
             // Using 0.0 might be confusing, but better than crashing or fake data.
             // Ideally the backend handles nulls or we wait for location.
             val lat = location?.latitude ?: 0.0
             val lng = location?.longitude ?: 0.0
             
             val currentUser = authRepository.currentUser
             val userId = currentUser?.id
             
             if (userId != null) {
                 val result = sosRepository.sendSos(
                     userId = userId,
                     circleId = null, // Backend infers active circle
                     message = pendingMessage,
                     lat = lat,
                     lng = lng
                 )
                 
                 if (result.isSuccess) {
                     // Success or failure, navigate to help screen
                 } else {
                     _uiState.update { it.copy(sendError = result.exceptionOrNull()?.message) }
                 }
             } else {
                  _uiState.update { it.copy(sendError = "User not authenticated") }
             }
             
             _uiState.update { 
                 it.copy(
                     isSending = false,
                     sosState = SosState.NavigateToAiHelp // Trigger navigation
                 ) 
             }
        }
    }

    fun onNavigatedToAiHelp() {
        pendingMessage = "I need help!"
        _uiState.update { it.copy(sosState = SosState.Idle) }
    }

    private fun sendResetToService() {
        val intent = Intent(appContext, BackgroundSafetyService::class.java).apply {
            action = BackgroundSafetyService.ACTION_RESET_DETECTOR
        }
        appContext.startService(intent)
    }

    private fun isInDismissCooldown(): Boolean {
        val cooldownMs = 5_000L // Avoid immediate re-triggers right after dismissal
        return (System.currentTimeMillis() - lastDismissedAt) < cooldownMs
    }

    override fun onCleared() {
        super.onCleared()
        holdJob?.cancel()
        countdownJob?.cancel()
    }
}
