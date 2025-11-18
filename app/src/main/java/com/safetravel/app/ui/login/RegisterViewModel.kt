package com.safetravel.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.model.RegisterRequest
import com.safetravel.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val fullName: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val registrationError: String? = null,
    val registrationSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    // Update functions for UI fields
    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }
    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value) }
    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullName = value) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }
    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value) }

    fun onRegisterClick() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, registrationError = null) }

        // Basic Validation
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(isLoading = false, registrationError = "Passwords do not match.") }
            return
        }
        if (state.username.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(isLoading = false, registrationError = "Please fill in all fields.") }
            return
        }

        viewModelScope.launch {
            // Create the request object
            val request = RegisterRequest(
                username = state.username,
                email = state.email,
                phone = state.phone,
                password = state.password,
                fullName = state.fullName,
                avatarUrl = "https://example.com/default_avatar.jpg" // Hardcoded for now
            )

            val result = authRepository.register(request)

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, registrationSuccess = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        //registrationError = result.exceptionOrNull()?.message ?: "Registration failed"
                        registrationError = "Registration failed, problem could be:\n " + "Not existing Email or already used Email\n" + "Already existed Username\n" + "Password too short (< 8 characters)"
                    )
                }
            }
        }
    }
}