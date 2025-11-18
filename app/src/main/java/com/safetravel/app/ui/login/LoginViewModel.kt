package com.safetravel.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "", // Changed from email to username for API
    val password: String = "",
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun onLoginClick() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(isLoading = true, loginError = null)

        viewModelScope.launch {
            val result = authRepository.login(currentState.username, currentState.password)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, loginSuccess = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    //loginError = result.exceptionOrNull()?.message ?: "Login failed"
                    loginError = "Login failed, unauthorized"
                )
            }
        }
    }
}