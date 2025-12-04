package com.safetravel.app.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.app.data.model.NotificationResponse
import com.safetravel.app.data.model.SosAlertResponse
import com.safetravel.app.data.repository.NotificationRepository
import com.safetravel.app.data.repository.SosRepository
import com.safetravel.app.ui.profile.SosAlertsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SosAlertsViewModel @Inject constructor(
    private val sosRepository: SosRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SosAlertsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Fetch SOS Alerts
            val alertsResult = sosRepository.getMySosAlerts()
            val alerts = alertsResult.getOrDefault(emptyList())
            
            // Fetch Notifications
            val notificationsResult = notificationRepository.getNotifications()
            val notifications = notificationsResult.getOrDefault(emptyList())
            
            _uiState.update { 
                it.copy(
                    alerts = alerts, 
                    notifications = notifications,
                    isLoading = false
                ) 
            }
        }
    }
    
    fun resolveAlert(alertId: Int) {
         viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = sosRepository.updateSosStatus(alertId, "resolved")
            if (result.isSuccess) {
                loadData() // Refresh list
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun markNotificationAsRead(notificationId: Int) {
        viewModelScope.launch {
            val result = notificationRepository.markAsRead(notificationId)
            if (result.isSuccess) {
                // Optimistically update UI
                val updatedList = _uiState.value.notifications.map { 
                    if (it.id == notificationId) it.copy(isRead = true) else it 
                }
                _uiState.update { it.copy(notifications = updatedList) }
            }
        }
    }
    
    fun deleteNotification(notificationId: Int) {
        viewModelScope.launch {
            val result = notificationRepository.deleteNotification(notificationId)
            if (result.isSuccess) {
                val updatedList = _uiState.value.notifications.filterNot { it.id == notificationId }
                _uiState.update { it.copy(notifications = updatedList) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosAlertsScreen(
    viewModel: SosAlertsViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 for SOS, 1 for Notifications

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerts & Notifications") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SOS Alerts") 
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            // Show badge count for unread
                            val unreadCount = uiState.notifications.count { !it.isRead }
                            Text(if (unreadCount > 0) "Notifications ($unreadCount)" else "Notifications")
                        }
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    if (selectedTab == 0) {
                        SosAlertsList(alerts = uiState.alerts, onResolve = viewModel::resolveAlert)
                    } else {
                        NotificationsList(
                            notifications = uiState.notifications,
                            onMarkRead = viewModel::markNotificationAsRead,
                            onDelete = viewModel::deleteNotification
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SosAlertsList(alerts: List<SosAlertResponse>, onResolve: (Int) -> Unit) {
    if (alerts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No SOS alerts history.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(alerts) { alert ->
                SosAlertItem(alert, onResolve = { onResolve(alert.id) })
            }
        }
    }
}

@Composable
fun NotificationsList(
    notifications: List<NotificationResponse>,
    onMarkRead: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    if (notifications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No notifications.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(notifications) { notification ->
                NotificationItem(
                    notification = notification,
                    onClick = { onMarkRead(notification.id) },
                    onDelete = { onDelete(notification.id) }
                )
                Divider()
            }
        }
    }
}

@Composable
fun SosAlertItem(alert: SosAlertResponse, onResolve: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.status == "resolved") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        border = if (alert.status != "resolved") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (alert.status == "resolved") "RESOLVED" else "SOS ACTIVE",
                    color = if (alert.status == "resolved") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(text = alert.createdAt ?: "", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Message: ${alert.message}", style = MaterialTheme.typography.bodyMedium)
            
            if (alert.status != "resolved") {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onResolve,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Mark as Resolved")
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: NotificationResponse,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notification.isRead) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.createdAt ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
