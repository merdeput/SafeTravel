package com.safetravel.app.ui.sos

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    LaunchedEffect(uiState.emergencyStopped) {
        if (uiState.emergencyStopped) {
            onEmergencyStopped()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("AI Emergency Assistant") }) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Quick Actions section
            EmergencyUtilsSection()
            
            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // Chat messages take up the remaining space
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(uiState.messages.reversed()) { chatMessage ->
                    Text(chatMessage.message, modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // Controls are now at the bottom, but within the safe area
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { showStopDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Stop Emergency")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.currentQuery,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("Ask for help...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        // Add padding to the button to prevent it from touching the edge
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            Button(onClick = viewModel::onAskClick, enabled = !uiState.isAwaitingResponse) {
                                Text("Ask")
                            }
                        }
                    }
                )
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
private fun EmergencyUtilsSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EmergencyCallButton(
                label = "Police",
                phoneNumber = "113",
                icon = Icons.Default.LocalPolice,
                modifier = Modifier.weight(1f)
            )
            EmergencyCallButton(
                label = "Ambulance",
                phoneNumber = "115",
                icon = Icons.Default.MedicalServices,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmergencyCallButton(
    label: String,
    phoneNumber: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dialIntent = remember {
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
    }

    Card(
        onClick = {
            try {
                context.startActivity(dialIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open dialer.", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = phoneNumber,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }
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
