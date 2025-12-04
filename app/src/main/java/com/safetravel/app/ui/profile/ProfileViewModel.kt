package com.safetravel.app.ui.profile

import androidx.lifecycle.ViewModel
import com.safetravel.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// The data classes (Trip, ProfileUiState) are now correctly defined in ProfileData.kt

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val user = authRepository.currentUser
        val displayName = user?.fullName ?: user?.username ?: "Guest"

        _uiState.update {
            it.copy(
                userName = displayName,
                trips = listOf(
                    Trip("1", "Paris, France", "2024-12-20"),
                    Trip("2", "Tokyo, Japan", "2025-01-15"),
                )
            )
        }
    }
}
