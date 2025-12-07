package com.safetravel.app.ui.triphistory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.model.TripDTO
import com.safetravel.app.data.model.User
import com.safetravel.app.data.repository.CircleRepository
import com.safetravel.app.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TripHistoryUiState(
    val trip: TripDTO? = null,
    val members: List<User> = emptyList(),
    val duration: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TripHistoryViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val circleRepository: CircleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tripId: Int = savedStateHandle.get<Int>("tripId")!!

    private val _uiState = MutableStateFlow(TripHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTripHistory()
    }

    private fun loadTripHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val tripResult = tripRepository.getTripById(tripId)

            if (tripResult.isFailure) {
                _uiState.update { it.copy(error = "Failed to load trip details.", isLoading = false) }
                return@launch
            }

            val trip = tripResult.getOrThrow()
            var members = emptyList<User>()

            if (trip.circleId != null) {
                val membersResult = circleRepository.getCircleMembers(trip.circleId)
                if (membersResult.isSuccess) {
                    members = membersResult.getOrThrow()
                }
            }

            val durationString = calculateDuration(trip.startDate, trip.endDate)

            _uiState.update {
                it.copy(
                    trip = trip,
                    members = members,
                    duration = durationString,
                    isLoading = false
                )
            }
        }
    }

    private fun calculateDuration(start: String, end: String?): String {
        if (end == null) return "-"
        try {
            val startDate = LocalDateTime.parse(start, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endDate = LocalDateTime.parse(end, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val duration = Duration.between(startDate, endDate)

            val days = duration.toDays()
            val hours = duration.toHours() % 24

            return when {
                days > 0 -> "$days day(s) and $hours hour(s)"
                hours > 0 -> "$hours hour(s)"
                else -> "Less than an hour"
            }
        } catch (e: Exception) {
            return "-"
        }
    }
}
