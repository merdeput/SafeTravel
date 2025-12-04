package com.safetravel.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onCreateTrip: () -> Unit,
    onManageContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSosAlerts: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                actions = {
                    // SOS Alerts Icon
                    IconButton(onClick = onNavigateToSosAlerts) {
                        Icon(Icons.Default.Warning, contentDescription = "SOS Alerts", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = onManageContacts) {
                        Icon(Icons.Default.Contacts, contentDescription = "Manage Contacts")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTrip) {
                Icon(Icons.Default.Add, contentDescription = "Create New Trip")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(16.dp)
        ) {
            Text("Welcome, ${uiState.userName}!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Text("My Trips", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.trips.isEmpty()) {
                Text("You haven't planned any trips yet.", modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.trips) { trip ->
                        TripItem(trip = trip, onClick = { /* TODO: Navigate to trip details */ })
                    }
                }
            }
        }
    }
}

@Composable
private fun TripItem(trip: Trip, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(trip.destination, style = MaterialTheme.typography.titleMedium)
                Text(trip.date, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
