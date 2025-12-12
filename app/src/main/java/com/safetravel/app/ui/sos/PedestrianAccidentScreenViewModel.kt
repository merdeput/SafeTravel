package com.safetravel.app.ui.sos

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.repository.SensorDataRepository
import com.safetravel.app.service.BackgroundSafetyService
import com.safetravel.app.ui.sos.data.DetectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PedestrianAccidentScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SensorDataRepository
) : ViewModel() {

    // Observe latest state from Repository (driven by Background Service)
    val detectionState = repository.latestDetectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetectionState())

    val paperStateName = repository.paperStateName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "IDLE")

    val sensorDataDeque = repository.sensorDataDeque
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArrayDeque())

    // Combined Alert State
    private val _accidentDetected = MutableStateFlow(false)
    val accidentDetected = _accidentDetected.asStateFlow()

    private val _detectionTime = MutableStateFlow(0L)
    val detectionTime = _detectionTime.asStateFlow()

    // Countdown Logic State
    private val _isCountdownActive = MutableStateFlow(false) // Legacy debug dialog; kept for UI compatibility
    val isCountdownActive = _isCountdownActive.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(30)
    val countdownSeconds = _countdownSeconds.asStateFlow()

    private val _showPasscodeDialog = MutableStateFlow(false)
    val showPasscodeDialog = _showPasscodeDialog.asStateFlow()

    private val _passcodeError = MutableStateFlow<String?>(null)
    val passcodeError = _passcodeError.asStateFlow()

    private var countdownJob: Job? = null

    init {
        // Monitor for accidents from the repository state
        viewModelScope.launch {
            detectionState.collect { state ->
                if (state.accidentConfirmed) {
                    checkForAccident()
                }
            }
        }
    }

    private fun checkForAccident() {
        // Avoid launching local debug countdown; global SOS flow handles alerts.
        if (!_accidentDetected.value) {
            _accidentDetected.value = true
            _detectionTime.value = System.currentTimeMillis()
        }
    }

    private fun startCountdown() {
        _isCountdownActive.value = true
        _countdownSeconds.value = 30
        
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 30 downTo 1) {
                _countdownSeconds.value = i
                delay(1000)
            }
            // Timeout reached -> Confirm Accident
            confirmAccident()
        }
    }

    private fun confirmAccident() {
        _isCountdownActive.value = false
        _showPasscodeDialog.value = false
        _accidentDetected.value = true
        _detectionTime.value = System.currentTimeMillis()
    }

    fun onImOkayClick() {
        // Local dialog no longer drives real alerts; keep no-op for UI compatibility
        _showPasscodeDialog.value = false
    }

    fun onVerifyPasscode(passcode: String) {
        if (passcode == "1234") { // Dummy passcode
            countdownJob?.cancel()
            resetAlertState()
            
            // Send reset command to Background Service
            val intent = Intent(context, BackgroundSafetyService::class.java).apply {
                action = BackgroundSafetyService.ACTION_RESET_DETECTOR
            }
            context.startService(intent)
        } else {
            _passcodeError.value = "Invalid passcode"
        }
    }
    
    fun onPasscodeDialogDismiss() {
        _showPasscodeDialog.value = false
    }

    fun onSendHelpClick() {
        countdownJob?.cancel()
        confirmAccident()
    }

    fun onReset() {
        resetAlertState()
        
        // Send reset command to Background Service
        val intent = Intent(context, BackgroundSafetyService::class.java).apply {
            action = BackgroundSafetyService.ACTION_RESET_DETECTOR
        }
        context.startService(intent)
    }

    private fun resetAlertState() {
        _accidentDetected.value = false
        _detectionTime.value = 0L
        _isCountdownActive.value = false
        _showPasscodeDialog.value = false
        countdownJob?.cancel()
    }
}
