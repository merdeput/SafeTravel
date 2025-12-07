package com.safetravel.app.ui.trip_live

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.model.SPECIAL_END_DATE
import com.safetravel.app.data.model.TripBase
import com.safetravel.app.data.repository.AuthRepository
import com.safetravel.app.data.repository.CircleRepository
import com.safetravel.app.data.repository.FriendRepository
import com.safetravel.app.data.repository.TripRepository
import com.safetravel.app.ui.profile.Circle
import com.safetravel.app.ui.profile.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TripManagementUiState(
    val circle: Circle? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val tripEnded: Boolean = false,
    val availableContacts: List<Contact> = emptyList(),
    val currentUserId: Int? = null // Add current user ID
)

@HiltViewModel
class TripManagementViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    private val friendRepository: FriendRepository,
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripManagementUiState())
    val uiState = _uiState.asStateFlow()

    private val argCircleId: Int? = savedStateHandle.get<Int>("circleId")
    private var resolvedCircleId: Int? = null

    init {
        // Set the current user ID in the state
        _uiState.update { it.copy(currentUserId = authRepository.currentUser?.id) }
        resolveActiveCircleAndLoad()
        loadFriends()
    }

    private fun resolveActiveCircleAndLoad() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val currentUser = authRepository.currentUser
            if (currentUser?.id == null) {
                _uiState.update { it.copy(error = "User not logged in.", isLoading = false) }
                return@launch
            }

            val tripsResult = tripRepository.getTripsByUser(currentUser.id)
            if (tripsResult.isSuccess) {
                val trips = tripsResult.getOrThrow()
                val activeTrip = trips.find { 
                    it.endDate == null || it.endDate == SPECIAL_END_DATE 
                }

                if (activeTrip != null && activeTrip.circleId != null) {
                    resolvedCircleId = activeTrip.circleId
                }
            }

            if (resolvedCircleId == null) {
                resolvedCircleId = argCircleId
            }

            if (resolvedCircleId != null) {
                loadCircleDetails(resolvedCircleId!!)
            } else {
                _uiState.update { it.copy(error = "No active trip circle found.", isLoading = false) }
            }
        }
    }

    private suspend fun loadCircleDetails(circleId: Int) {
        val circleResult = circleRepository.getCircle(circleId)
        if (circleResult.isSuccess) {
            val circleResponse = circleResult.getOrThrow()
            val membersResult = circleRepository.getCircleMembers(circleId)
            val members = membersResult.getOrDefault(emptyList())
            val memberIds = members.mapNotNull { it.id?.toString() }

            _uiState.update {
                it.copy(
                    circle = Circle(
                        id = circleResponse.id.toString(),
                        name = circleResponse.circleName,
                        description = circleResponse.description,
                        memberIds = memberIds,
                        members = members
                    ),
                    isLoading = false
                )
            }
        } else {
            _uiState.update { it.copy(error = "Failed to load circle details.", isLoading = false) }
        }
    }

    private fun loadFriends() {
        viewModelScope.launch {
            val friendsResult = friendRepository.getFriends()
            if (friendsResult.isSuccess) {
                val friends = friendsResult.getOrDefault(emptyList())
                val contacts = friends.map { user ->
                    Contact(
                        id = user.id?.toString() ?: user.username ?: "",
                        name = user.fullName ?: user.username ?: "Unknown",
                        phone = user.phone ?: "",
                        username = user.username ?: ""
                    )
                }
                _uiState.update { it.copy(availableContacts = contacts) }
            }
        }
    }

    fun addMembersToCircle(memberIdsToAdd: List<String>) {
        if (resolvedCircleId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val errors = mutableListOf<String>()

            memberIdsToAdd.forEach { memberIdStr ->
                val memberId = memberIdStr.toIntOrNull()
                if (memberId != null) {
                    val result = circleRepository.addCircleMember(resolvedCircleId!!, memberId)
                    if (result.isFailure) {
                        errors.add("Failed to add user $memberId")
                    }
                } else {
                    errors.add("Invalid user ID: $memberIdStr")
                }
            }

            if (errors.isNotEmpty()) {
                _uiState.update { it.copy(error = errors.joinToString("\n")) }
            } else {
                loadCircleDetails(resolvedCircleId!!) // Refresh on success
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun removeMemberFromCircle(memberId: String) {
        if (resolvedCircleId == null) return

        val userId = memberId.toIntOrNull()
        if (userId != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val result = circleRepository.removeCircleMember(resolvedCircleId!!, userId)
                if (result.isSuccess) {
                    loadCircleDetails(resolvedCircleId!!)
                } else {
                    _uiState.update { it.copy(error = "Failed to remove member.") }
                }
                _uiState.update { it.copy(isLoading = false) }
            }
        } else {
            _uiState.update { it.copy(error = "Invalid user ID for removal.") }
        }
    }

    fun endTrip() {
        if (resolvedCircleId == null) {
            _uiState.update { it.copy(error = "Cannot end trip: No Circle ID found.") }
            return
        }

        val currentUser = authRepository.currentUser
        if (currentUser?.id == null) {
             _uiState.update { it.copy(error = "Cannot end trip: User not logged in.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val tripsResult = tripRepository.getTripsByUser(currentUser.id)
            if (tripsResult.isFailure) {
                _uiState.update { it.copy(error = "Failed to fetch trips to end.", isLoading = false) }
                return@launch
            }

            val allTrips = tripsResult.getOrThrow()
            val activeTrip = allTrips.find { 
                //it.circleId == resolvedCircleId &&
                (it.endDate == null || it.endDate == SPECIAL_END_DATE)
            }

            if (activeTrip == null) {
                _uiState.update { it.copy(error = "No active trip found for this circle.", isLoading = false) }
                return@launch
            }

            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            
            val updatedTripBase = TripBase(
                userId = activeTrip.userId,
                tripName = activeTrip.tripName,
                destination = activeTrip.destination,
                startDate = activeTrip.startDate,
                endDate = now, 
                tripType = activeTrip.tripType,
                haveElderly = activeTrip.haveElderly,
                haveChildren = activeTrip.haveChildren,
                circleId = activeTrip.circleId,
                notes = activeTrip.notes
            )

            val updateResult = tripRepository.updateTrip(activeTrip.id, updatedTripBase)

            if (updateResult.isSuccess) {
                _uiState.update { it.copy(tripEnded = true, isLoading = false) }
            } else {
                _uiState.update { it.copy(error = "Failed to end trip: ${updateResult.exceptionOrNull()?.message}", isLoading = false) }
            }
        }
    }
    
     fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
