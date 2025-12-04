package com.safetravel.app.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
    var showCreateCircleDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var circleToModify by remember { mutableStateOf<Circle?>(null) }

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
                title = { Text("Friends & Circles") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
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
                Section(
                    title = "My Friends",
                    buttonText = "Add Friend",
                    onButtonClick = { showAddFriendDialog = true }
                ) {
                    if (uiState.friends.isEmpty()) {
                        Text("No friends yet. Add someone by username!", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        uiState.friends.forEach { friend ->
                            FriendItem(friend)
                        }
                    }
                }
            }

            // --- Circles Section ---
            item {
                Section(title = "My Circles", buttonText = "Create Circle", onButtonClick = { showCreateCircleDialog = true }) {
                    uiState.circles.forEach { circle ->
                        CircleItem(
                            circle = circle,
                            allContacts = uiState.contacts,
                            onAddMembersClick = { circleToModify = circle },
                            onDeleteCircleClick = { viewModel.deleteCircle(circle.id) },
                            onRemoveMember = { memberId -> viewModel.removeMemberFromCircle(circle.id, memberId) }
                        )
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

    if (showCreateCircleDialog) {
        CreateCircleDialog(
            onCreate = { viewModel.createCircle(it); showCreateCircleDialog = false },
            onDismiss = { showCreateCircleDialog = false }
        )
    }

    circleToModify?.let {
        AddMembersDialog(
            circle = it,
            allContacts = uiState.contacts,
            onAddMembers = { memberIds -> viewModel.addMembersToCircle(it.id, memberIds) },
            onDismiss = { circleToModify = null }
        )
    }
}

@Composable
private fun FriendRequestItem(request: FriendRequest, onAccept: () -> Unit, onReject: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                IconButton(onClick = onAccept) {
                    Icon(Icons.Default.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onReject) {
                    Icon(Icons.Default.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun FriendItem(friend: User) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                val displayName = friend.fullName ?: friend.username ?: "Unknown"
                Text(displayName, style = MaterialTheme.typography.titleMedium)
                if (!friend.fullName.isNullOrEmpty() && !friend.username.isNullOrEmpty()) {
                    Text(friend.username, style = MaterialTheme.typography.bodySmall)
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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true
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

@Composable
private fun Section(title: String, buttonText: String, onButtonClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(onClick = onButtonClick) { Text(buttonText) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun CircleItem(
    circle: Circle, 
    allContacts: List<Contact>, 
    onAddMembersClick: () -> Unit,
    onDeleteCircleClick: () -> Unit,
    onRemoveMember: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(circle.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${circle.members.size} members", style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = onDeleteCircleClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Circle", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    if (circle.members.isEmpty()) {
                        Text("No members yet.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        circle.members.forEach { member ->
                             val displayName = member.fullName ?: member.username ?: "User ID: ${member.id}"
                             val displayUsername = member.username ?: ""
                             val memberId = member.id?.toString() ?: ""
                             
                             Row(
                                 verticalAlignment = Alignment.CenterVertically,
                                 modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                             ) {
                                 Column(modifier = Modifier.weight(1f)) {
                                     Text(displayName)
                                     if (displayUsername.isNotEmpty() && displayName != displayUsername) {
                                        Text("@$displayUsername", style = MaterialTheme.typography.bodySmall)
                                     }
                                 }
                                 IconButton(onClick = { onRemoveMember(memberId) }, modifier = Modifier.size(24.dp)) {
                                     Icon(Icons.Default.Close, contentDescription = "Remove Member")
                                 }
                             }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onAddMembersClick, modifier = Modifier.fillMaxWidth()) {
                        Text("Add Members")
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCircleDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Circle") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Circle Name") }) },
        confirmButton = { Button(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddMembersDialog(circle: Circle, allContacts: List<Contact>, onAddMembers: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val eligibleContacts = remember(allContacts, circle.memberIds) {
        allContacts.filter { it.id !in circle.memberIds }
    }
    
    val selectedMemberIds = remember { mutableStateOf<Set<String>>(emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Members to ${circle.name}") },
        text = {
            if (eligibleContacts.isEmpty()) {
                Text("All your friends are already in this circle.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(eligibleContacts) { contact ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val currentSelection = selectedMemberIds.value.toMutableSet()
                                    if (contact.id in currentSelection) {
                                        currentSelection.remove(contact.id)
                                    } else {
                                        currentSelection.add(contact.id)
                                    }
                                    selectedMemberIds.value = currentSelection
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(checked = contact.id in selectedMemberIds.value, onCheckedChange = null)
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(contact.name)
                                if (contact.username.isNotEmpty()) {
                                    Text("@${contact.username}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = { onAddMembers(selectedMemberIds.value.toList()); onDismiss() },
                enabled = selectedMemberIds.value.isNotEmpty()
            ) { 
                Text("Add Selected") 
            } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
