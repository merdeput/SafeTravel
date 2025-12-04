package com.safetravel.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.model.CircleResponse
import com.safetravel.app.data.model.FriendRequest
import com.safetravel.app.data.model.User
import com.safetravel.app.data.repository.CircleRepository
import com.safetravel.app.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val circleRepository: CircleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // 1. Fetch friends
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
                _uiState.update { it.copy(friends = friends, contacts = contacts) }
            }

            // 2. Fetch pending requests (with enrichment logic)
            val requestsResult = friendRepository.getPendingFriendRequests()
            if (requestsResult.isSuccess) {
                val requests = requestsResult.getOrDefault(emptyList())
                val enrichedRequests = requests.map { request ->
                    if (request.sender == null && request.senderId != null) {
                        val userResult = friendRepository.getUserById(request.senderId)
                        if (userResult.isSuccess) {
                            request.copy(sender = userResult.getOrNull())
                        } else {
                            request
                        }
                    } else {
                        request
                    }
                }
                _uiState.update { it.copy(pendingRequests = enrichedRequests) }
            }

            // 3. Fetch Circles
            loadCircles()

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadCircles() {
        val circlesResult = circleRepository.getCircles()
        if (circlesResult.isSuccess) {
            val rawCircles = circlesResult.getOrDefault(emptyList())
            
            val uiCircles = rawCircles.map { circleResponse ->
                val membersResult = circleRepository.getCircleMembers(circleResponse.id)
                val members = membersResult.getOrDefault(emptyList())
                val memberIds = members.mapNotNull { it.id?.toString() }
                
                Circle(
                    id = circleResponse.id.toString(),
                    name = circleResponse.circleName,
                    description = circleResponse.description,
                    memberIds = memberIds,
                    members = members
                )
            }
            _uiState.update { it.copy(circles = uiCircles) }
        }
    }

    // --- Circle Management ---

    fun createCircle(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = circleRepository.createCircle(name, "Created from app")
            if (result.isSuccess) {
                loadCircles()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun deleteCircle(circleId: String) {
         viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = circleRepository.deleteCircle(circleId.toInt())
            if (result.isSuccess) {
                loadCircles()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun addMembersToCircle(circleId: String, memberIdsToAdd: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val errors = mutableListOf<String>()
            memberIdsToAdd.forEach { memberIdStr ->
                val memberId = memberIdStr.toIntOrNull()
                if (memberId != null) {
                     val result = circleRepository.addCircleMember(circleId.toInt(), memberId)
                     if (result.isFailure) {
                         errors.add("Failed to add user $memberId")
                     }
                }
            }
            loadCircles()
            if (errors.isNotEmpty()) {
                 _uiState.update { it.copy(error = "Some members could not be added.") }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun removeMemberFromCircle(circleId: String, memberId: String) {
        val userId = memberId.toIntOrNull()
        if (userId != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val result = circleRepository.removeCircleMember(circleId.toInt(), userId)
                if (result.isSuccess) {
                    loadCircles()
                } else {
                     _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
                }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // --- Friend Management ---

    fun sendFriendRequest(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = friendRepository.sendFriendRequest(username)
            _uiState.update { it.copy(isLoading = false) }
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun acceptFriendRequest(requestId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = friendRepository.acceptFriendRequest(requestId)
            if (result.isSuccess) {
                loadData()
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun rejectFriendRequest(requestId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = friendRepository.rejectFriendRequest(requestId)
            if (result.isSuccess) {
                loadData()
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
