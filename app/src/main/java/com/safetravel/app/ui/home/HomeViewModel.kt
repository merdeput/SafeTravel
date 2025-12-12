package com.safetravel.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.model.SPECIAL_END_DATE
import com.safetravel.app.data.model.TripDTO
import com.safetravel.app.data.repository.AuthRepository
import com.safetravel.app.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val currentTrip: TripDTO? = null,
    val userName: String = "",
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val user = authRepository.currentUser
        _uiState.update { it.copy(userName = user?.fullName ?: user?.username ?: "Traveler") }
        checkActiveTrip()
    }

    fun checkActiveTrip() {
        val userId = authRepository.currentUser?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = tripRepository.getTripsByUser(userId)
            if (result.isSuccess) {
                val trips = result.getOrThrow()
                val activeTrip = trips.find { isTripActive(it) }
                _uiState.update { it.copy(isLoading = false, currentTrip = activeTrip) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    private fun isTripActive(trip: TripDTO): Boolean {
        val endStr = trip.endDate
        if (endStr == null || endStr == SPECIAL_END_DATE) return true
        return try {
             val end = LocalDateTime.parse(endStr, DateTimeFormatter.ISO_DATE_TIME)
             LocalDateTime.now().isBefore(end)
        } catch (e: Exception) {
            try {
                 val end = LocalDateTime.parse(endStr)
                 LocalDateTime.now().isBefore(end)
            } catch (e2: Exception) {
                false
            }
        }
    }
}
