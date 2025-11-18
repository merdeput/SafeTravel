package com.safetravel.app.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showCreateCircleDialog by remember { mutableStateOf(false) }
    var circleToModify by remember { mutableStateOf<Circle?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Contacts & Circles") },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.padding(it).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Circles Section ---
            item {
                Section(title = "My Circles", buttonText = "Create New Circle", onButtonClick = { showCreateCircleDialog = true }) {
                    uiState.circles.forEach { circle ->
                        CircleItem(circle, uiState.contacts, onAddMembersClick = { circleToModify = circle })
                    }
                }
            }

            // --- Contacts Section ---
            item {
                Section(title = "My Contacts", buttonText = "Add New Contact", onButtonClick = { showAddContactDialog = true }) {
                    uiState.contacts.forEach { contact ->
                        ContactItem(contact, onDeleteClick = { viewModel.deleteContact(contact.id) })
                    }
                }
            }
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            onAdd = { viewModel.addContact(it.first, it.second); showAddContactDialog = false },
            onDismiss = { showAddContactDialog = false }
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
private fun CircleItem(circle: Circle, allContacts: List<Contact>, onAddMembersClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val members = remember(circle.memberIds, allContacts) {
        allContacts.filter { it.id in circle.memberIds }
    }

    Card(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Text(circle.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${members.size} members")
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    members.forEach { member ->
                        Text("  - ${member.name}")
                    }
                    if (members.isEmpty()) {
                        Text("No members yet.", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onAddMembersClick, modifier = Modifier.fillMaxWidth()) {
                        Text("Add / Remove Members")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactItem(contact: Contact, onDeleteClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name)
            Text(contact.phone, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Contact", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun AddContactDialog(onAdd: (Pair<String, String>) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Contact") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") })
            }
        },
        confirmButton = { Button(onClick = { onAdd(name to phone) }, enabled = name.isNotBlank() && phone.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
    val selectedMemberIds = remember { mutableStateOf(circle.memberIds.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Members to ${circle.name}") },
        text = {
            LazyColumn {
                items(allContacts) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                        val currentSelection = selectedMemberIds.value.toMutableSet()
                        if (it.id in currentSelection) currentSelection.remove(it.id) else currentSelection.add(it.id)
                        selectedMemberIds.value = currentSelection
                    }) {
                        Checkbox(checked = it.id in selectedMemberIds.value, onCheckedChange = null)
                        Text(it.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onAddMembers(selectedMemberIds.value.toList()); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}