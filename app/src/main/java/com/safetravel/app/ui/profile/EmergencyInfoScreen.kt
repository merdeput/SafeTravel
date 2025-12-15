package com.safetravel.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safetravel.app.data.repository.EmergencyInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyInfoScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Local state for editing form, initialized from ViewModel state
    var name by remember(uiState.emergencyInfo) { mutableStateOf(uiState.emergencyInfo.name) }
    var bloodType by remember(uiState.emergencyInfo) { mutableStateOf(uiState.emergencyInfo.bloodType) }
    var allergies by remember(uiState.emergencyInfo) { mutableStateOf(uiState.emergencyInfo.allergies) }
    var conditions by remember(uiState.emergencyInfo) { mutableStateOf(uiState.emergencyInfo.conditions) }
    var contactName by remember(uiState.emergencyInfo) { mutableStateOf(uiState.emergencyInfo.contactName) }
    var contactPhone by remember(uiState.emergencyInfo) { mutableStateOf(uiState.emergencyInfo.contactPhone) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Info") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val newInfo = EmergencyInfo(
                        name = name,
                        bloodType = bloodType,
                        allergies = allergies,
                        conditions = conditions,
                        contactName = contactName,
                        contactPhone = contactPhone
                    )
                    viewModel.onEmergencyInfoChange(newInfo)
                    onNavigateUp()
                }
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "This information will be displayed on the lock screen during an emergency.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = bloodType,
                onValueChange = { bloodType = it },
                label = { Text("Blood Type") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = allergies,
                onValueChange = { allergies = it },
                label = { Text("Allergies (if any)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = conditions,
                onValueChange = { conditions = it },
                label = { Text("Medical Conditions") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Text("Emergency Contact", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            
            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Contact Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = contactPhone,
                onValueChange = { contactPhone = it },
                label = { Text("Contact Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(60.dp)) // Space for FAB
        }
    }
}
