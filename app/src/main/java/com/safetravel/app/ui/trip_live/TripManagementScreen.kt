package com.safetravel.app.ui.trip_live

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safetravel.app.ui.profile.AddMembersDialog

@Composable
fun TripManagementScreen(
    viewModel: TripManagementViewModel = hiltViewModel(),
    onEndTrip: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddMembersDialog by remember { mutableStateOf(false) }

    // Handle trip ended navigation
    LaunchedEffect(uiState.tripEnded) {
        if (uiState.tripEnded) {
            onEndTrip()
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                uiState.circle?.let { circle ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(circle.name, style = MaterialTheme.typography.headlineSmall)
                            }
                            if (!circle.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(circle.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Text(
                             "Members (${circle.members.size})", 
                             style = MaterialTheme.typography.titleMedium,
                             color = MaterialTheme.colorScheme.onSurface
                         )
                         FilledIconButton(
                             onClick = { showAddMembersDialog = true },
                             colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                         ) {
                             Icon(Icons.Default.PersonAdd, contentDescription = "Add Members")
                         }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(circle.members) { member ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, 
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    val displayName = member.fullName ?: member.username ?: "User ID: ${member.id}"
                                    Text(displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.removeMemberFromCircle(member.id.toString()) }) {
                                        Icon(
                                            Icons.Default.Close, 
                                            contentDescription = "Remove Member",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                } ?: run {
                     // If no circle data yet
                     if (!uiState.isLoading) {
                         Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                             Text("No trip information available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                         }
                     } else {
                         Spacer(modifier = Modifier.weight(1f))
                     }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.endTrip() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("End Trip", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (showAddMembersDialog && uiState.circle != null) {
             AddMembersDialog(
                circle = uiState.circle!!,
                allContacts = uiState.availableContacts,
                onAddMembers = { members ->
                    viewModel.addMembersToCircle(members)
                    showAddMembersDialog = false
                },
                onDismiss = { showAddMembersDialog = false }
            )
        }
    }
}
