
package com.safetravel.app.ui.createtrip

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(viewModel: CreateTripViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Plan a New Trip") })
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Input Form
            OutlinedTextField(
                value = uiState.where,
                onValueChange = viewModel::onWhereChange,
                label = { Text("Where are you going?") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = uiState.time,
                onValueChange = viewModel::onTimeChange,
                label = { Text("When (e.g., \"Next weekend\")") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = uiState.duration,
                onValueChange = viewModel::onDurationChange,
                label = { Text("How long (e.g., \"3 days\")") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            // Special People Checkboxes
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Traveling with")
                Checkbox(checked = uiState.hasElderly, onCheckedChange = viewModel::onHasElderlyChange)
                Text("Elderly")
                Spacer(modifier = Modifier.width(16.dp))
                Checkbox(checked = uiState.hasChildren, onCheckedChange = viewModel::onHasChildrenChange)
                Text("Children")
            }

            // Trip Type Dropdown
            TripTypeDropdown(uiState.tripType, viewModel::onTripTypeChange)

            // Generate Button
            Button(
                onClick = viewModel::generateSafetyReport,
                enabled = !uiState.isGenerating,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Generate Safety Report")
                }
            }

            // Generated Report
            uiState.generatedReport?.let {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripTypeDropdown(selectedType: String, onTypeChange: (String) -> Unit) {
    val tripTypes = listOf("Adventure", "Sightseeing", "Business", "Relaxation", "Family")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedType,
            onValueChange = {}, // read-only
            label = { Text("Trip Type") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tripTypes.forEach { type ->
                DropdownMenuItem(text = { Text(type) }, onClick = {
                    onTypeChange(type)
                    expanded = false
                })
            }
        }
    }
}
