package com.safetravel.app.ui.sos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHelpScreen(
    viewModel: AiHelpViewModel = hiltViewModel(),
    onEmergencyStopped: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStopDialog by remember { mutableStateOf(false) }

    // Navigate back when emergency is stopped
    LaunchedEffect(uiState.emergencyStopped) {
        if (uiState.emergencyStopped) {
            onEmergencyStopped()
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("AI Emergency Assistant") }) },
        bottomBar = {
            Column {
                Button(
                    onClick = { showStopDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Stop Emergency")
                }
                // Chat Input
                OutlinedTextField(
                    value = uiState.currentQuery,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("Ask for help...") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                    trailingIcon = {
                        Button(onClick = viewModel::onAskClick, enabled = !uiState.isAwaitingResponse) {
                            Text("Ask")
                        }
                    }
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.padding(it).padding(16.dp),
            reverseLayout = true
        ) {
            items(uiState.messages.reversed()) { chatMessage ->
                Text(chatMessage.message)
            }
        }
    }

    if (showStopDialog) {
        StopEmergencyDialog(
            passcodeError = uiState.passcodeError,
            onConfirm = viewModel::onVerifyPasscode,
            onDismiss = { showStopDialog = false }
        )
    }
}

@Composable
private fun StopEmergencyDialog(
    passcodeError: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stop Emergency") },
        text = {
            Column {
                Text("Enter your passcode to confirm you are safe and stop the emergency alert.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text("Passcode") },
                    isError = passcodeError != null
                )
                passcodeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(passcode) }) {
                Text("Confirm & Stop")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

