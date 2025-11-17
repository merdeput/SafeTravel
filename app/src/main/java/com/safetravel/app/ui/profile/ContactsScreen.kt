package com.safetravel.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Contacts & Circles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Circles Section ---
            item {
                Section(title = "My Circles") {
                    uiState.circles.forEach { circle ->
                        Text("  - ${circle.name} (${circle.memberIds.size} members)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showCreateCircleDialog = true }) {
                        Text("Create New Circle")
                    }
                }
            }

            // --- Contacts Section ---
            item {
                Section(title = "My Contacts") {
                    uiState.contacts.forEach { contact ->
                        Text("  - ${contact.name} (${contact.phone})")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showAddContactDialog = true }) {
                        Text("Add New Contact")
                    }
                }
            }
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            onAdd = {
                viewModel.addContact(it.first, it.second)
                showAddContactDialog = false
            },
            onDismiss = { showAddContactDialog = false }
        )
    }

    if (showCreateCircleDialog) {
        CreateCircleDialog(
            onCreate = {
                viewModel.createCircle(it)
                showCreateCircleDialog = false
            },
            onDismiss = { showCreateCircleDialog = false }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Column(content = content)
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
        confirmButton = {
            Button(onClick = { onAdd(name to phone) }, enabled = name.isNotBlank() && phone.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CreateCircleDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Circle") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Circle Name") })
        },
        confirmButton = {
            Button(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
