package com.safetravel.app.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val user = authRepository.currentUser
        val displayName = user?.fullName ?: user?.username ?: "Guest"

        _uiState.update {
            it.copy(userName = displayName)
        }
        
        if (user != null && user.id != null) {
            fetchTrips(user.id)
        }
    }

    private fun fetchTrips(userId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = tripRepository.getTripsByUser(userId)
            
            result.onSuccess { tripsDto ->
                processTrips(tripsDto)
            }.onFailure { error ->
                Log.e("ProfileViewModel", "Error fetching trips", error)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load trips: ${error.message}"
                    ) 
                }
            }
        }
    }
    
    private fun processTrips(tripsDto: List<TripDTO>) {
        try {
            // ISO 8601 format often comes as yyyy-MM-ddTHH:mm:ss or similar.
            // If strictly dates, maybe yyyy-MM-dd.
            // Adjust formatter if needed. Python's datetime usually serializes to ISO format.
            val now = LocalDateTime.now()
            
            val mappedTrips = tripsDto.map { dto ->
                // Try parsing. If it fails, might need a specific formatter.
                // Assuming standard ISO 8601 for now.
                // If the string is just "yyyy-MM-dd", LocalDateTime.parse might fail without time.
                // Let's try to be robust or assume a format.
                // Given python datetime, it usually includes T.
                
                val start = try {
                    LocalDateTime.parse(dto.startDate, DateTimeFormatter.ISO_DATE_TIME)
                } catch (e: Exception) {
                    try {
                        // Fallback for date only or other formats if necessary
                         LocalDateTime.parse(dto.startDate) // Default parser
                    } catch (e2: Exception) {
                        now // Fallback/Error handling
                    }
                }
                
                val end = try {
                    LocalDateTime.parse(dto.endDate, DateTimeFormatter.ISO_DATE_TIME)
                } catch (e: Exception) {
                     try {
                         LocalDateTime.parse(dto.endDate)
                    } catch (e2: Exception) {
                        now
                    }
                }
                
                val status = when {
                    now.isBefore(start) -> TripStatus.UPCOMING
                    now.isAfter(end) -> TripStatus.COMPLETED
                    else -> TripStatus.ONGOING
                }
                
                Trip(
                    id = dto.id,
                    destination = dto.destination,
                    startDate = dto.startDate, // Keep original string for display? Or format it?
                    endDate = dto.endDate,
                    status = status
                )
            }
            
            // "trip that is happening"
            val current = mappedTrips.firstOrNull { it.status == TripStatus.ONGOING }
            
            // "trips that have been through"
            val past = mappedTrips.filter { it.status == TripStatus.COMPLETED }
                .sortedByDescending { it.endDate } // Most recent past trip first

            _uiState.update { 
                it.copy(
                    isLoading = false,
                    currentTrip = current,
                    pastTrips = past
                ) 
            }
            
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error processing trips", e)
             _uiState.update { 
                it.copy(isLoading = false, error = "Error processing trips") 
            }
        }
    }
}
