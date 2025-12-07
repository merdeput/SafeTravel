package com.safetravel.app.ui.profile

import com.safetravel.app.data.model.CircleMemberResponse
import com.safetravel.app.data.model.CircleResponse
import com.safetravel.app.data.model.FriendRequest
import com.safetravel.app.data.model.NotificationResponse
import com.safetravel.app.data.model.SosAlertResponse
import com.safetravel.app.data.model.User

// --- Data Models for Profile, Contacts and Circles ---

data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val username: String = ""
)

data class Circle(
    val id: String,
    val name: String,
    val memberIds: List<String>,
    val description: String? = null,
    val members: List<User> = emptyList()
)

data class Trip(
    val id: Int,
    val destination: String,
    val startDate: String,
    val endDate: String?,
    val status: TripStatus,
    val circleId: Int? // Added to link navigation
)

enum class TripStatus {
    UPCOMING,
    ONGOING,
    COMPLETED
}

// --- UI State Models ---

data class ProfileUiState(
    val userName: String = "",
    val currentTrip: Trip? = null,
    val pastTrips: List<Trip> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val circles: List<Circle> = emptyList(),
    val pendingRequests: List<FriendRequest> = emptyList(),
    val friends: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SosAlertsUiState(
    val alerts: List<SosAlertResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val notifications: List<NotificationResponse> = emptyList() 
)
