package com.safetravel.app.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.model.SPECIAL_END_DATE
import com.safetravel.app.data.model.TripDTO
import com.safetravel.app.data.repository.AuthRepository
import com.safetravel.app.data.repository.CircleRepository
import com.safetravel.app.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
    private val tripRepository: TripRepository,
    private val circleRepository: CircleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()
    
    private val currentUserId: Int? = authRepository.currentUser?.id

    init {
        val user = authRepository.currentUser
        val displayName = user?.fullName ?: user?.username ?: "Guest"

        _uiState.update {
            it.copy(userName = displayName)
        }
        
        if (currentUserId != null) {
            fetchData(currentUserId)
        }
    }

    private fun fetchData(userId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val tripsDeferred = async { tripRepository.getTripsByUser(userId) }
            val circlesDeferred = async { circleRepository.getCircles() }
            
            val tripsResult = tripsDeferred.await()
            val circlesResult = circlesDeferred.await()
            
            if (tripsResult.isSuccess) {
                val tripsDto = tripsResult.getOrThrow()
                val circles = circlesResult.getOrDefault(emptyList())
                processTrips(tripsDto, circles)
            } else {
                val error = tripsResult.exceptionOrNull()
                Log.e("ProfileViewModel", "Error fetching data", error)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load data: ${error?.message}"
                    ) 
                }
            }
        }
    }
    
    fun deleteTrip(tripId: Int) {
        viewModelScope.launch {
            val result = tripRepository.deleteTrip(tripId)
            if (result.isSuccess) {
                // Refresh the list after deletion
                if (currentUserId != null) {
                    fetchData(currentUserId)
                }
            } else {
                val error = result.exceptionOrNull()
                 _uiState.update { 
                    it.copy(error = "Failed to delete trip: ${error?.message}") 
                }
            }
        }
    }
    
    private fun processTrips(tripsDto: List<TripDTO>, circles: List<com.safetravel.app.data.model.CircleResponse>) {
        try {
            val now = LocalDateTime.now()
            
            val mappedTrips = tripsDto.map { dto ->
                val endStr = dto.endDate
                val end = if (endStr != null && endStr != SPECIAL_END_DATE) {
                    try {
                        LocalDateTime.parse(endStr, DateTimeFormatter.ISO_DATE_TIME)
                    } catch (e: Exception) {
                         try {
                             LocalDateTime.parse(endStr)
                        } catch (e2: Exception) {
                            null
                        }
                    }
                } else {
                    null 
                }
                
                val status = if (end == null) {
                    TripStatus.ONGOING
                } else if (now.isAfter(end)) {
                    TripStatus.COMPLETED
                } else {
                    TripStatus.COMPLETED
                }
                
                var resolvedCircleId = dto.circleId
                if (resolvedCircleId == null && status == TripStatus.ONGOING) {
                    val nameMatch = circles.find { 
                        it.circleName.equals("Trip to ${dto.destination}", ignoreCase = true) 
                    }
                    if (nameMatch != null) {
                        resolvedCircleId = nameMatch.id
                    } else {
                        resolvedCircleId = circles.maxByOrNull { it.id }?.id
                    }
                }

                Trip(
                    id = dto.id,
                    destination = dto.destination,
                    startDate = dto.startDate, 
                    endDate = if (endStr == SPECIAL_END_DATE) null else endStr, 
                    status = status,
                    circleId = resolvedCircleId
                )
            }
            
            val current = mappedTrips.firstOrNull { it.status == TripStatus.ONGOING }
            val past = mappedTrips.filter { it.status == TripStatus.COMPLETED }
                .sortedByDescending { it.endDate } 

            _uiState.update { 
                it.copy(
                    isLoading = false,
                    currentTrip = current,
                    pastTrips = past,
                    error = null // Clear previous errors on success
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
