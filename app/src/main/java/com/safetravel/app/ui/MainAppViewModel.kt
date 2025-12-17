package com.safetravel.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.model.NewsWeatherResponse
import com.safetravel.app.data.model.SPECIAL_END_DATE
import com.safetravel.app.data.repository.AuthRepository
import com.safetravel.app.data.repository.NewsWeatherRepository
import com.safetravel.app.data.repository.NotificationRepository
import com.safetravel.app.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainAppUiState(
    val unreadNotificationCount: Int = 0,
    val activeTripWeatherReport: NewsWeatherResponse? = null,
    val isLoadingReport: Boolean = false
)

@HiltViewModel
class MainAppViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    private val newsWeatherRepository: NewsWeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainAppUiState())
    val uiState = _uiState.asStateFlow()

    init {
        startPollingNotifications()
        fetchActiveTripReport()
    }

    private fun startPollingNotifications() {
        viewModelScope.launch {
            while (true) {
                fetchUnreadCount()
                delay(15000) // Poll every 15 seconds
            }
        }
    }

    fun fetchUnreadCount() {
        viewModelScope.launch {
            val result = notificationRepository.getNotifications()
            if (result.isSuccess) {
                val notifications = result.getOrDefault(emptyList())
                val unreadCount = notifications.count { !it.isRead }
                _uiState.update { it.copy(unreadNotificationCount = unreadCount) }
            }
        }
    }

    fun fetchActiveTripReport() {
        viewModelScope.launch {
            val user = authRepository.currentUser ?: return@launch
            if (user.id == null) return@launch
            
            // 1. Get user trips
            val tripsResult = tripRepository.getTripsByUser(user.id)
            if (tripsResult.isSuccess) {
                val trips = tripsResult.getOrNull() ?: return@launch
                // Find active trip (Assuming only one active trip, or taking the latest one)
                // Logic for "Active" could be endDate is SPECIAL_END_DATE or in the future
                val activeTrip = trips.find { it.endDate == SPECIAL_END_DATE } ?: trips.firstOrNull()
                
                if (activeTrip != null) {
                    val destination = activeTrip.destination
                    
                    // 2. Fetch Report for this destination
                    // We check if we already have it in memory or cache (Here we just fetch since we don't have persistent cache yet)
                    _uiState.update { it.copy(isLoadingReport = true) }
                    
                    val reportResult = newsWeatherRepository.getWeatherPlace(destination)
                    
                    if (reportResult.isSuccess) {
                        _uiState.update { 
                            it.copy(
                                activeTripWeatherReport = reportResult.getOrNull(),
                                isLoadingReport = false
                            ) 
                        }
                    } else {
                        _uiState.update { it.copy(isLoadingReport = false) }
                    }
                }
            }
        }
    }
}
