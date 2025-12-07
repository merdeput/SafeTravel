package com.safetravel.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onCreateTrip: () -> Unit,
    onManageContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSosAlerts: () -> Unit,
    onNavigateToInTrip: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tripToDelete by remember { mutableStateOf<Trip?>(null) }

    if (showDeleteDialog && tripToDelete != null) {
        DeleteConfirmationDialog(
            tripName = tripToDelete!!.destination,
            onConfirm = {
                viewModel.deleteTrip(tripToDelete!!.id)
                showDeleteDialog = false
                tripToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                tripToDelete = null
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SafeTravel", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTrip,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Trip")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { ProfileHeader(userName = uiState.userName) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuickActionCard(Icons.Default.Warning, "SOS Alerts", onNavigateToSosAlerts, MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    QuickActionCard(Icons.Default.Contacts, "Contacts", onManageContacts, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }

            if (uiState.isLoading) {
                item { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else {
                if (uiState.currentTrip != null) {
                    item { Text("Current Trip", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    item {
                        TripItem(
                            trip = uiState.currentTrip!!,
                            isCurrent = true,
                            onClick = {
                                val navigationId = uiState.currentTrip!!.circleId ?: uiState.currentTrip!!.id
                                onNavigateToInTrip(navigationId)
                            },
                            onDelete = {
                                tripToDelete = uiState.currentTrip
                                showDeleteDialog = true
                            }
                        )
                    }
                } else if (uiState.pastTrips.isEmpty()) {
                    item { EmptyTripsState() }
                }

                if (uiState.pastTrips.isNotEmpty()) {
                    item { Text("Past Trips", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    items(uiState.pastTrips) { trip ->
                        TripItem(
                            trip = trip,
                            isCurrent = false,
                            onClick = { /* TODO: Navigate to trip history */ },
                            onDelete = {
                                tripToDelete = trip
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            if (uiState.error != null) {
                item { Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(tripName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Trip") },
        text = { Text("Are you sure you want to delete the trip to $tripName? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ProfileHeader(userName: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(userName.firstOrNull()?.toString()?.uppercase() ?: "U", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(userName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Traveler", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickActionCard(icon: ImageVector, label: String, onClick: () -> Unit, color: Color, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyTripsState() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No trips planned", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Create a new trip to get started with safety monitoring.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripItem(trip: Trip, isCurrent: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(trip.destination, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
                Spacer(modifier = Modifier.height(4.dp))
                val dateText = try {
                    if (trip.endDate != null) {
                        "${trip.startDate.split("T")[0]} - ${trip.endDate.split("T")[0]}"
                    } else {
                        "Started: ${trip.startDate.split("T")[0]}"
                    }
                } catch (e: Exception) { trip.startDate }
                Text(dateText, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.8f))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Trip", tint = MaterialTheme.colorScheme.error)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = "Details", tint = contentColor)
        }
    }
}
