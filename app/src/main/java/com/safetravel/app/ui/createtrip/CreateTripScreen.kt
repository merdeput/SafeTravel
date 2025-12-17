package com.safetravel.app.ui.createtrip

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safetravel.app.data.model.NewsWeatherResponse
import com.safetravel.app.data.model.TravelAdvice
import com.safetravel.app.data.model.WeatherForecast
import java.text.SimpleDateFormat
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

    // Navigation is handled by the parent (MainActivity) observing uiState.createdCircleId
    // We just trigger the action via the button.

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

            Text("This trip includes:", style = typography.titleMedium)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating (approx 30s)...")
                    }
                } else {
                    Text("Generate Safety Report")
                }
            }

            // --- Generated Report ---
            uiState.generatedReport?.let { report ->
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                SafetyReportWidget(report)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Show Error if any
            if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Start Trip Button - Only visible when report is generated
            if (uiState.generatedReport != null) {
                Button(
                    onClick = { viewModel.onStartTripClick() },
                    enabled = !uiState.isCreatingTrip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isCreatingTrip) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Start Trip")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        DatePickerWithState(onDateSelected = viewModel::onTimeChange) { showDatePicker = false }
    }
}

@Composable
fun SafetyReportWidget(report: NewsWeatherResponse) {
    val province = report.provinces?.firstOrNull() ?: return
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Safety Report for ${province.provinceName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            if (province.score != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Safety Score: ",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${province.score}/100",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (province.score > 70) Color(0xFF4CAF50) else Color(0xFFFF9800) // Green or Orange
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Executive Summary
            if (!province.executiveSummary.isNullOrBlank()) {
                Text(text = "Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = province.executiveSummary, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Weather
            if (!province.weatherForecast.isNullOrEmpty()) {
                Text(text = "Weather Forecast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(province.weatherForecast) { weather ->
                         WeatherCard(weather)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Advice
            if (!province.travelAdvice.isNullOrEmpty()) {
                Text(text = "Travel Advice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                province.travelAdvice.forEach { advice ->
                    AdviceItem(advice)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun WeatherCard(weather: WeatherForecast) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.width(140.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = weather.date ?: "", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = weather.temperature ?: "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = weather.condition ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}

@Composable
fun AdviceItem(advice: TravelAdvice) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (advice.category == "An toàn") Icons.Default.Warning else Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                if (!advice.category.isNullOrBlank()) {
                    Text(text = advice.category, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Text(text = advice.advice ?: "", style = MaterialTheme.typography.bodyMedium)
            }
        }
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
