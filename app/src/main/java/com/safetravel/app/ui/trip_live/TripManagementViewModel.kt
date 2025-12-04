package com.safetravel.app.ui.trip_live

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.model.User
import com.safetravel.app.data.repository.CircleRepository
import com.safetravel.app.data.repository.FriendRepository
import com.safetravel.app.ui.profile.Circle
import com.safetravel.app.ui.profile.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class TripManagementUiState(
    val circle: Circle? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val tripEnded: Boolean = false,
    val availableContacts: List<Contact> = emptyList()
)

@HiltViewModel
class TripManagementViewModel @Inject constructor(
    private val circleRepository: CircleRepository,
    private val friendRepository: FriendRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripManagementUiState())
    val uiState = _uiState.asStateFlow()

    // Retrieve circleId from SavedStateHandle. Since this ViewModel is used within a nested navigation graph
    // (the bottom tabs of MainScreen), we need to make sure the arguments are passed correctly.
    // However, since MainScreen itself received the arguments, they might not be automatically propagated 
    // to the nested graph's ViewModels unless we explicitly pass them or the graph is scoped correctly.
    // But typically, HiltViewModel in a nested graph won't see the parent's arguments unless we do something extra.
    // For now, let's check if we can get it. If null, we have a problem.
    private val circleId: Int? = savedStateHandle.get<Int>("circleId")

    init {
        loadCircleDetails()
        loadFriends()
    }

    private fun loadCircleDetails() {
        if (circleId == null) {
             // If circleId is missing, it's likely because the navigation argument wasn't passed down
             // to this nested graph destination.
            _uiState.update { it.copy(error = "Cannot find circle_id for this circle") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
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
        if (circleId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val errors = mutableListOf<String>()

            memberIdsToAdd.forEach { memberIdStr ->
                val memberId = memberIdStr.toIntOrNull()
                if (memberId != null) {
                    val result = circleRepository.addCircleMember(circleId, memberId)
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
                loadCircleDetails() // Refresh on success
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun removeMemberFromCircle(memberId: String) {
        if (circleId == null) return

        val userId = memberId.toIntOrNull()
        if (userId != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val result = circleRepository.removeCircleMember(circleId, userId)
                if (result.isSuccess) {
                    loadCircleDetails()
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
        // Here you would typically call a repository to end the trip on the backend.
        // For now, we just update the UI state to trigger navigation.
        viewModelScope.launch {
            _uiState.update { it.copy(tripEnded = true) }
        }
    }
    
     fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
