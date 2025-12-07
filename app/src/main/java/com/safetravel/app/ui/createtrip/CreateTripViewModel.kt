package com.safetravel.app.ui.createtrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.model.TripBase
import com.safetravel.app.data.repository.AuthRepository
import com.safetravel.app.data.repository.CircleRepository
import com.safetravel.app.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CreateTripUiState(
    val where: String = "",
    val time: String = "",
    val duration: String = "",
    val tripType: String = "",
    val hasElderly: Boolean = false,
    val hasChildren: Boolean = false,
    val isGenerating: Boolean = false,
    val generatedReport: String? = null,
    val isCreatingTrip: Boolean = false,
    val createdCircleId: Int? = null, // Restored for navigation in MainActivity
    val error: String? = null
)

@HiltViewModel
class CreateTripViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTripUiState())
    val uiState = _uiState.asStateFlow()

    fun onWhereChange(newWhere: String) {
        _uiState.update { it.copy(where = newWhere) }
    }

    fun onTimeChange(newTime: String) {
        _uiState.update { it.copy(time = newTime) }
    }

    fun onDurationChange(newDuration: String) {
        _uiState.update { it.copy(duration = newDuration) }
    }

    fun onTripTypeChange(newType: String) {
        _uiState.update { it.copy(tripType = newType) }
    }

    fun onHasElderlyChange(newValue: Boolean) {
        _uiState.update { it.copy(hasElderly = newValue) }
    }

    fun onHasChildrenChange(newValue: Boolean) {
        _uiState.update { it.copy(hasChildren = newValue) }
    }

    fun generateSafetyReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            delay(2000) // Simulate API call
            val report = """
                ## Safety Report for ${_uiState.value.where}
                
                **General Advisory**: This location is generally safe, but exercise caution in crowded areas.
                
                **Weather**: Expect sunny weather with a chance of rain in the evening.
                
                **Emergency Contacts**: 
                * Local Police: 113
                * Ambulance: 115
                
                **Tips**:
                * Keep your valuables secure.
                * Stay hydrated.
                ${if (_uiState.value.hasElderly) "* **Accessibility**: Check for wheelchair ramps." else ""}
                ${if (_uiState.value.hasChildren) "* **Family**: Keep children close in busy markets." else ""}
            """.trimIndent()
            
            _uiState.update { it.copy(generatedReport = report, isGenerating = false) }
        }
    }

    fun onStartTripClick() {
        if (_uiState.value.createdCircleId != null) return 

        val currentUser = authRepository.currentUser
        if (currentUser?.id == null) {
             _uiState.update { it.copy(error = "User not logged in.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingTrip = true, error = null) }
            
            // 1. Create Circle
            val circleName = "Trip to ${_uiState.value.where}"
            val circleResult = circleRepository.createCircle(circleName, "Circle for trip to ${_uiState.value.where}")
            
            if (circleResult.isFailure) {
                _uiState.update { it.copy(error = "Failed to create circle: ${circleResult.exceptionOrNull()?.message}", isCreatingTrip = false) }
                return@launch
            }
            
            val circleId = circleResult.getOrThrow().id

            // 2. Prepare Trip Data
            val (startDate, endDate) = calculateDates(_uiState.value.time, _uiState.value.duration)
            
            val tripBase = TripBase(
                userId = currentUser.id,
                tripName = "Trip to ${_uiState.value.where}",
                destination = _uiState.value.where,
                startDate = startDate,
                endDate = endDate,
                tripType = _uiState.value.tripType.ifBlank { "Leisure" }, // Default
                haveElderly = _uiState.value.hasElderly,
                haveChildren = _uiState.value.hasChildren,
                circleId = circleId,
                notes = "Auto-generated trip"
            )

            // 3. Create Trip
            val tripResult = tripRepository.createTrip(tripBase)
            
            if (tripResult.isSuccess) {
                _uiState.update { it.copy(createdCircleId = circleId, isCreatingTrip = false) }
            } else {
                _uiState.update { it.copy(error = "Failed to create trip: ${tripResult.exceptionOrNull()?.message}", isCreatingTrip = false) }
            }
        }
    }
    
    private fun calculateDates(dateStr: String, durationStr: String): Pair<String, String> {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        val start = try {
            LocalDate.parse(dateStr, formatter).atStartOfDay()
        } catch (e: Exception) {
            LocalDate.now().atStartOfDay()
        }

        val daysToAdd = when {
            durationStr.contains("1-2 days") -> 2L
            durationStr.contains("3-4 days") -> 4L
            durationStr.contains("1 week") -> 7L
            durationStr.contains("2 weeks") -> 14L
            durationStr.contains("1 month") -> 30L
            else -> 1L // Default
        }
        
        val end = start.plusDays(daysToAdd)
        
        return Pair(start.format(isoFormatter), end.format(isoFormatter))
    }
    
    fun onTripCreationNavigated() {
        _uiState.update { it.copy(createdCircleId = null, error = null) }
    }
}
