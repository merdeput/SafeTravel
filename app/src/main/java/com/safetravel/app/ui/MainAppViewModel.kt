package com.safetravel.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainAppUiState(
    val unreadNotificationCount: Int = 0
)

@HiltViewModel
class MainAppViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainAppUiState())
    val uiState = _uiState.asStateFlow()

    init {
        startPollingNotifications()
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
}
