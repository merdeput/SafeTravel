package com.safetravel.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.repository.AuthRepository
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
    val passcode: String = "1234"
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
            // Load initial settings from DataStore
            val settings = settingsRepository.settingsFlow.first()
            _uiState.update {
                it.copy(
                    countdownTime = settings.countdownTime,
                    passcode = settings.passcode
                )
            }
        }
    }

    fun onCountdownTimeChange(newTime: String) {
        val time = newTime.toIntOrNull() ?: 30
        _uiState.update { it.copy(countdownTime = time) }
        viewModelScope.launch { settingsRepository.saveCountdownTime(time) }
    }

    fun onPasscodeChange(newPasscode: String) {
        _uiState.update { it.copy(passcode = newPasscode) }
        viewModelScope.launch { settingsRepository.savePasscode(newPasscode) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
