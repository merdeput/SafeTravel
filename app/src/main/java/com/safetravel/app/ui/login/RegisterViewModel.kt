
package com.safetravel.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val registrationError: String? = null,
    val registrationSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email) }
    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password) }
    fun onConfirmPasswordChange(confirm: String) = _uiState.update { it.copy(confirmPassword = confirm) }

    fun onRegisterClick() {
        _uiState.update { it.copy(isLoading = true, registrationError = null) }
        viewModelScope.launch {
            delay(1500) // Simulate network call
            val state = _uiState.value
            if (state.password != state.confirmPassword) {
                _uiState.update { it.copy(isLoading = false, registrationError = "Passwords do not match.") }
            } else if (state.email.isBlank() || state.password.isBlank()) {
                _uiState.update { it.copy(isLoading = false, registrationError = "Email and password cannot be blank.") }
            } else {
                _uiState.update { it.copy(isLoading = false, registrationSuccess = true) }
            }
        }
    }
}
