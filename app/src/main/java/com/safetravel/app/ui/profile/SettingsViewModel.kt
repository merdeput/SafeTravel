package com.safetravel.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.repository.AuthRepository
import com.safetravel.app.data.repository.EmergencyInfo
import com.safetravel.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val countdownTime: Int = 30,
    val passcode: String = "",
    val emergencyInfo: EmergencyInfo = EmergencyInfo()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { 
                    it.copy(
                        countdownTime = settings.countdownTime,
                        passcode = settings.passcode,
                        emergencyInfo = settings.emergencyInfo
                    ) 
                }
            }
        }
    }

    fun onCountdownTimeChange(newValue: String) {
        val time = newValue.toIntOrNull() ?: return
        viewModelScope.launch {
            settingsRepository.saveCountdownTime(time)
        }
    }

    fun onPasscodeChange(newValue: String) {
        viewModelScope.launch {
            settingsRepository.savePasscode(newValue)
        }
    }
    
    fun onEmergencyInfoChange(newInfo: EmergencyInfo) {
        viewModelScope.launch {
            settingsRepository.saveEmergencyInfo(newInfo)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
