package com.safetravel.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safetravel.app.data.model.FriendRequest
import com.safetravel.app.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddFriendDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
             FloatingActionButton(onClick = { showAddFriendDialog = true }) {
                 Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend")
             }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Friend Requests Section ---
            if (uiState.pendingRequests.isNotEmpty()) {
                item {
                    Text("Friend Requests", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.pendingRequests.forEach { request ->
                            FriendRequestItem(
                                request = request,
                                onAccept = { viewModel.acceptFriendRequest(request.id) },
                                onReject = { viewModel.rejectFriendRequest(request.id) }
                            )
                        }
                    }
                }
            }

            // --- Friends Section ---
            item {
                Text("My Friends", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (uiState.friends.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No friends yet. Add someone by username!", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.friends.forEach { friend ->
                            FriendItem(friend)
                        }
                    }
                }
            }
        }
    }

    if (showAddFriendDialog) {
        AddFriendDialog(
            onAdd = {
                viewModel.sendFriendRequest(it)
                showAddFriendDialog = false
            },
            onDismiss = { showAddFriendDialog = false }
        )
    }
}

@Composable
private fun FriendRequestItem(request: FriendRequest, onAccept: () -> Unit, onReject: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val senderName = request.sender?.username 
                    ?: request.sender?.fullName 
                    ?: "User ID: ${request.senderId}"
                
                Text(text = senderName, style = MaterialTheme.typography.titleMedium)
                Text(text = "Sent you a friend request", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                FilledIconButton(onClick = onAccept, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Default.Check, contentDescription = "Accept")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(onClick = onReject, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Close, contentDescription = "Reject")
                }
            }
        }
    }
}

@Composable
private fun FriendItem(friend: User) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val initial = (friend.username?.firstOrNull() ?: '?').toString().uppercase()
                    Text(initial, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                val displayName = friend.fullName ?: friend.username ?: "Unknown"
                Text(displayName, style = MaterialTheme.typography.titleMedium)
                if (!friend.fullName.isNullOrEmpty() && !friend.username.isNullOrEmpty()) {
                    Text("@${friend.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AddFriendDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var username by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Friend") },
        text = {
            Column {
                Text("Enter the username of the friend you want to add.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(username) },
                enabled = username.isNotBlank()
            ) {
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
