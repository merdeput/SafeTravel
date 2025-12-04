package com.safetravel.app.ui.trip_live

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safetravel.app.ui.profile.AddMembersDialog
import com.safetravel.app.ui.profile.Contact
import com.safetravel.app.ui.profile.Circle

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
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            uiState.circle?.let { circle ->
                Text(circle.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                     Text("Members (${circle.members.size})", style = MaterialTheme.typography.titleMedium)
                     Button(onClick = { showAddMembersDialog = true }) {
                         Text("Add Members")
                     }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(circle.members) { member ->
                        val displayName = member.fullName ?: member.username ?: "User ID: ${member.id}"
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(displayName, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.removeMemberFromCircle(member.id.toString()) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Member")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.endTrip() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("End Trip")
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
}
