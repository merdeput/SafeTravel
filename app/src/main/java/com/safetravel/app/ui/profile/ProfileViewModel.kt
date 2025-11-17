package com.safetravel.app.ui.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class Trip(
    val id: String,
    val destination: String,
    val date: String
)

data class ProfileUiState(
    val userName: String = "Jane Doe", // Dummy data
    val trips: List<Trip> = emptyList()
)

@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Load dummy trip data
        _uiState.value = _uiState.value.copy(
            trips = listOf(
                Trip("1", "Paris, France", "2024-12-20"),
                Trip("2", "Tokyo, Japan", "2025-01-15"),
                Trip("3", "New York, USA", "2025-03-10")
            )
        )
    }
}
