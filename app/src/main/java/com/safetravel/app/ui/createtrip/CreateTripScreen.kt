package com.safetravel.app.ui.createtrip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    viewModel: CreateTripViewModel = hiltViewModel(),
    onStartTrip: () -> Unit,
    onNavigateToLocationPicker: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Plan Your New Trip") })
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- Input Form ---
            OutlinedTextField(
                value = uiState.where,
                onValueChange = viewModel::onWhereChange, // Allow user to type manually
                label = { Text("Where are you going?") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Select Location on Map",
                        modifier = Modifier.clickable { onNavigateToLocationPicker() } // Open map on icon click
                    )
                }
            )

            DatePickerField(uiState.time, onDateSelected = viewModel::onTimeChange) { showDatePicker = true }
            DurationDropdown(uiState.duration, viewModel::onDurationChange)
            Spacer(modifier = Modifier.height(8.dp))
            TripTypeDropdown(uiState.tripType, viewModel::onTripTypeChange)
            Spacer(modifier = Modifier.height(16.dp))

            Text("This trip includes:", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Checkbox(checked = uiState.hasElderly, onCheckedChange = viewModel::onHasElderlyChange)
                Text("Elderly Members")
                Spacer(modifier = Modifier.weight(1f))
                Checkbox(checked = uiState.hasChildren, onCheckedChange = viewModel::onHasChildrenChange)
                Text("Children")
            }

            Button(
                onClick = viewModel::generateSafetyReport,
                enabled = !uiState.isGenerating && uiState.where.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Generate Safety Report")
                }
            }

            // --- Generated Report and Start Button ---
            uiState.generatedReport?.let {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onStartTrip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Trip")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        DatePickerWithState(onDateSelected = viewModel::onTimeChange) { showDatePicker = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerWithState(onDateSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = datePickerState)
    }

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            val sdf = SimpleDateFormat("MM/dd/yyyy", context.resources.configuration.locales[0])
            onDateSelected(sdf.format(Date(it)))
        }
    }
}

@Composable
private fun DatePickerField(selectedDate: String, onDateSelected: (String) -> Unit, onClick: () -> Unit) {
    OutlinedTextField(
        value = selectedDate,
        onValueChange = onDateSelected,
        label = { Text("When?") },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Select Date",
                modifier = Modifier.clickable { onClick() }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationDropdown(selectedDuration: String, onDurationChange: (String) -> Unit) {
    val durationOptions = listOf("1-2 days", "3-4 days", "1 week", "2 weeks", "1 month", "> 1 month")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedDuration,
            onValueChange = {}, // read-only
            label = { Text("How long?") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth().padding(bottom = 8.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            durationOptions.forEach { duration ->
                DropdownMenuItem(text = { Text(duration) }, onClick = {
                    onDurationChange(duration)
                    expanded = false
                })
            }
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
